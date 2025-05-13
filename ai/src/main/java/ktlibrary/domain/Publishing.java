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

    @PostPersist
    public void onPostPersist() {
        Published published = new Published(this);
        published.publishAfterCommit();
    }

    public static PublishingRepository repository() {
        PublishingRepository publishingRepository = AiApplication.applicationContext.getBean(
            PublishingRepository.class
        );
        return publishingRepository;
    }
}
//>>> DDD / Aggregate Root
