package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
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

    @Embedded
    private ManuscriptId manuscriptId;

    private String pdfPath;

    private Long authorId;

    public static PublishingRepository repository() {
        PublishingRepository publishingRepository = AiApplication.applicationContext.getBean(
            PublishingRepository.class
        );
        return publishingRepository;
    }

    //<<< Clean Arch / Port Method
    public static void publish(PublishingRequested publishingRequested) {
        //implement business logic here:

        /** Example 1:  new item 
        Publishing publishing = new Publishing();
        repository().save(publishing);

        Published published = new Published(publishing);
        published.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        // if publishingRequested.authorId exists, use it
        
        // ObjectMapper mapper = new ObjectMapper();
        // Map<Long, Object> manuscriptMap = mapper.convertValue(publishingRequested.getAuthorId(), Map.class);

        repository().findById(publishingRequested.get???()).ifPresent(publishing->{
            
            publishing // do something
            repository().save(publishing);

            Published published = new Published(publishing);
            published.publishAfterCommit();

         });
        */

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
