package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ktlibrary.AiApplication;
import ktlibrary.domain.Published;
import ktlibrary.service.AIService;
import ktlibrary.service.PDFService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "Publishing_table")
@Data
@Slf4j
//<<< DDD / Aggregate Root
public class Publishing {
    private static final Logger logger = LoggerFactory.getLogger(Publishing.class);
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Lob
    private String image;

    @Lob
    private String summaryContent;

    private String bookName;

    @Lob
    private String pdfPath;

    private String authorId;

    @Lob
    private String webUrl;

    private String category;

    public static PublishingRepository repository() {
        PublishingRepository publishingRepository = AiApplication.applicationContext.getBean(
            PublishingRepository.class
        );
        return publishingRepository;
    }

    //<<< Clean Arch / Port Method
    public static void publish(PublishingRequested publishingRequested) {
        // 동시에 여러 요청이 처리되는 것을 방지
        if (!isProcessing.compareAndSet(false, true)) {
            logger.warn("이미 출판 처리가 진행 중입니다. 요청이 무시됩니다.");
            return;
        }
        
        try {
            logger.info("\n===== AI 출판 처리 시작 =====");
            
            // 출판 정보 객체 생성
            Publishing publishing = new Publishing();
            publishing.setBookName(publishingRequested.getTitle());
            logger.info("책 제목: {}", publishingRequested.getTitle());
            
            // 서비스 인스턴스 가져오기
            AIService aiService = AiApplication.applicationContext.getBean(AIService.class);
            PDFService pdfService = AiApplication.applicationContext.getBean(PDFService.class);
            
            // 책 내용 가져오기
            String content = publishingRequested.getContent();
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("책 내용이 비어 있습니다.");
            }
            logger.info("책 내용 길이: {}자", content.length());
            
            // 1. 표지 이미지 생성을 위한 프롬프트 생성
            logger.info("1단계: 이미지 생성 프롬프트 생성 시작");
            String coverImagePrompt = aiService.generateCoverImagePrompt(content);
            logger.info("1단계 완료: 이미지 생성 프롬프트 - {}", coverImagePrompt);
            
            // 2. DALL-E API를 사용하여 실제 이미지 생성 및 URL 저장
            logger.info("2단계: 이미지 생성 API 호출 시작");
            String imageUrl = null;
            try {
                imageUrl = aiService.generateImage(coverImagePrompt);
                publishing.setImage(imageUrl);
                logger.info("2단계 완료: 이미지 URL 생성됨 - {}", imageUrl);
            } catch (Exception e) {
                // API 호출 실패 시 기본 이미지 사용
                logger.error("이미지 생성 API 호출 실패: {}", e.getMessage());
                publishing.setImage("https://kt-library.com/images/default-cover.jpg");
            }
            
            // 3. 장르 분류 및 저장
            logger.info("3단계: 카테고리 분류 시작");
            String category = aiService.categorizeContent(content);
            publishing.setCategory(category);
            logger.info("3단계 완료: 분류된 카테고리 - {}", category);
            
            // 4. 줄거리 요약 및 저장
            logger.info("4단계: 내용 요약 시작");
            String summary = aiService.summarizeContent(content);
            publishing.setSummaryContent(summary);
            logger.info("4단계 완료: 요약 완료 ({}자)", summary.length());
            
            // 5. 저자 정보 처리
            logger.info("5단계: 저자 정보 처리 시작");
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<Long, Object> authorMap = mapper.convertValue(publishingRequested.getAuthorId(), Map.class);
                Long authorId = Long.valueOf(authorMap.get("id").toString());
                logger.info("저자 ID: {}", authorId);
                
                RestTemplate restTemplate = new RestTemplate();
                String authorServiceUrl = "http://localhost:8082/authors/" + authorId;
                ResponseEntity<Map> authorResponse = restTemplate.getForEntity(authorServiceUrl, Map.class);
                
                publishing.setAuthorId(authorResponse.getBody().get("authorName").toString());
                logger.info("5단계 완료: 저자 이름 - {}", publishing.getAuthorId());
            } catch (Exception e) {
                logger.error("저자 정보 조회 실패: {}", e.getMessage());
                publishing.setAuthorId("알 수 없는 저자");
            }
            
            // 6. 모든 정보가 준비된 후 PDF 생성 (PDFService 직접 호출)
            logger.info("6단계: PDF 생성 시작");
            String pdfPath = pdfService.generatePdf(
                content, 
                publishing.getImage(), 
                publishing.getSummaryContent(), 
                publishing.getBookName());
            publishing.setPdfPath(pdfPath);
            logger.info("6단계 완료: PDF 생성됨 - {}", pdfPath);
            
            // PDF 파일 경로를 웹에서 접근 가능한 URL로 변환
            // 기존 webUrl을 PDF 파일 URL로 업데이트
            try {
                // 절대 경로를 상대 경로로 변환
                String relativePath = pdfPath.substring(pdfPath.indexOf("/storage"));
                // 웹 서버의 기본 URL에 PDF 경로 추가
                String pdfUrl = "http://localhost:8080" + relativePath;
                publishing.setWebUrl(pdfUrl);
                logger.info("PDF 웹 URL 업데이트: {}", pdfUrl);
            } catch (Exception e) {
                logger.error("PDF URL 생성 실패: {}", e.getMessage());
            }
    
            // 7. 출판 정보 저장
            logger.info("7단계: 출판 정보 저장 시작");
            repository().save(publishing);
            logger.info("7단계 완료: 출판 정보 저장됨");
    
            // 8. 이벤트 발행
            logger.info("8단계: 출판 이벤트 발행 시작");
            Published published = new Published(publishing);
            published.publishAfterCommit();
            logger.info("8단계 완료: 출판 이벤트 발행됨");
            
            logger.info("===== AI 출판 처리 완료 =====\n");
        } catch (Exception e) {
            logger.error("출판 처리 중 오류 발생: {}", e.getMessage(), e);
        } finally {
            // 처리 상태 초기화 - 다음 요청을 받을 수 있도록 함
            isProcessing.set(false);
        }
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
