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
import lombok.Data;

@Entity
@Table(name = "Publishing_table")
@Data
//<<< DDD / Aggregate Root
public class Publishing {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String image;

    private String summaryContent;

    private String bookName;

    private String pdfPath;

    private String authorId;

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

        Publishing publishing = new Publishing();
        publishing.setBookName(publishingRequested.getTitle());
        
        // AI 서비스 가져오기
        AIService aiService = AiApplication.applicationContext.getBean(AIService.class);
        
        // 책 내용 가져오기
        String content = publishingRequested.getContent();
        
        // 1. content 값을 pdf로 변환하고 웹 URL 생성
        String webUrl = aiService.convertToPdfAndGenerateWebUrl(content);
        publishing.setWebUrl(webUrl);
        
        // 2. 표지 이미지 생성을 위한 프롬프트 생성 및 이미지 URL 저장
        String coverImagePrompt = aiService.generateCoverImagePrompt(content);
        // 실제로는 이 프롬프트를 이미지 생성 API에 전송하여 URL을 받아야 함
        // 여기서는 시뮬레이션으로 프롬프트를 이미지 URL로 사용
        publishing.setImage("https://kt-library.com/images/" + System.currentTimeMillis());
        
        // 3. 장르 분류 및 저장
        String category = aiService.categorizeContent(content);
        publishing.setCategory(category);
        
        // 4. 줄거리 요약 및 저장
        String summary = aiService.summarizeContent(content);
        publishing.setSummaryContent(summary);
        
        // 5. PDF 경로 생성 및 저장
        String pdfPath = aiService.generatePdfPath(content, publishing.getImage(), summary);
        publishing.setPdfPath(pdfPath);

        // 저자 정보 처리
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Object> authorMap = mapper.convertValue(publishingRequested.getAuthorId(), Map.class);

        Long authorId = Long.valueOf(authorMap.get("id").toString());

        RestTemplate restTemplate = new RestTemplate();
        String authorServiceUrl = "http://localhost:8082/authors/" + authorId;

        ResponseEntity<Map> authorResponse = restTemplate.getForEntity(authorServiceUrl, Map.class);
        publishing.setAuthorId(authorResponse.getBody().get("authorName").toString());

        repository().save(publishing);

        Published published = new Published(publishing);
        published.publishAfterCommit();
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
