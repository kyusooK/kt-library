package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ktlibrary.AiApplication;
import ktlibrary.domain.Published;
import ktlibrary.service.AIService;
import ktlibrary.service.PDFService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "Publishing_table")
@Data
@Slf4j
//<<< DDD / Aggregate Root
public class Publishing {

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
        System.out.println("\n===== AI 출판 처리 시작 =====");
        
        Publishing publishing = new Publishing();
        publishing.setBookName(publishingRequested.getTitle());
        System.out.println("책 제목: " + publishingRequested.getTitle());
        
        // AI 서비스 가져오기
        AIService aiService = AiApplication.applicationContext.getBean(AIService.class);
        // PDF 서비스 가져오기
        PDFService pdfService = AiApplication.applicationContext.getBean(PDFService.class);
        
        // 책 내용 가져오기
        String content = publishingRequested.getContent();
        System.out.println("책 내용 길이: " + content.length() + "자");
        
        // 1. content 값을 기반으로 웹 URL 생성
        String webUrl = aiService.convertToPdfAndGenerateWebUrl(content);
        publishing.setWebUrl(webUrl);
        System.out.println("생성된 웹 URL: " + webUrl);
        
        // 2. 표지 이미지 생성을 위한 프롬프트 생성
        String coverImagePrompt = aiService.generateCoverImagePrompt(content);
        System.out.println("이미지 생성 프롬프트: " + coverImagePrompt);
        
        // 3. DALL-E API를 사용하여 실제 이미지 생성 및 URL 저장
        try {
            System.out.println("이미지 생성 API 호출 중...");
            String imageUrl = aiService.generateImage(coverImagePrompt);
            publishing.setImage(imageUrl);
            System.out.println("생성된 이미지 URL: " + imageUrl);
        } catch (Exception e) {
            // API 호출 실패 시 기본 이미지 사용
            publishing.setImage("https://kt-library.com/images/default-cover.jpg");
            System.err.println("이미지 생성 API 호출 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 4. 장르 분류 및 저장
        System.out.println("카테고리 분류 중...");
        String category = aiService.categorizeContent(content);
        publishing.setCategory(category);
        System.out.println("분류된 카테고리: " + category);
        
        // 5. 줄거리 요약 및 저장
        System.out.println("내용 요약 중...");
        String summary = aiService.summarizeContent(content);
        publishing.setSummaryContent(summary);
        System.out.println("요약된 내용: " + summary);
        
        // 6. 저자 정보 처리
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Object> authorMap = mapper.convertValue(publishingRequested.getAuthorId(), Map.class);

        Long authorId = Long.valueOf(authorMap.get("id").toString());
        System.out.println("저자 ID: " + authorId);

        RestTemplate restTemplate = new RestTemplate();
        String authorServiceUrl = "http://localhost:8082/authors/" + authorId;

        try {
            ResponseEntity<Map> authorResponse = restTemplate.getForEntity(authorServiceUrl, Map.class);
            publishing.setAuthorId(authorResponse.getBody().get("authorName").toString());
            System.out.println("저자 이름: " + publishing.getAuthorId());
        } catch (Exception e) {
            System.err.println("저자 정보 조회 실패: " + e.getMessage());
            publishing.setAuthorId("알 수 없는 저자");
        }
        
        // 7. 모든 정보가 준비된 후 PDF 생성 (PDFService 직접 호출)
        System.out.println("PDF 생성 시작...");
        String pdfPath = pdfService.generatePdf(content, publishing.getImage(), publishing.getSummaryContent(), publishing.getBookName());
        publishing.setPdfPath(pdfPath);
        System.out.println("생성된 PDF 경로: " + pdfPath);

        // 출판 정보 저장
        repository().save(publishing);
        System.out.println("출판 정보 저장 완료");

        // 이벤트 발행
        Published published = new Published(publishing);
        published.publishAfterCommit();
        System.out.println("출판 이벤트 발행 완료");
        
        System.out.println("===== AI 출판 처리 완료 =====\n");
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
