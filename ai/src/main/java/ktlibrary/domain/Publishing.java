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

        // 1. publishingRequested.getContent() 값을 pdf로 변환
        // 2. pdf를 web url(임시 변수에 저장)로 변환.
        // 3. web url을 토대로 표지 이미지 생성 후, url로 변환한 값을 image에 저장
        // 4. web url을 토대로 장르 분류 및 줄거리 요약 후, category, summaryContent에 저장.
        // 5. image, summaryContent, publishingRequested.getContent()를 토대로 pdf 처리
        // 6. webURL, pdfPath를 저장


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
