package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import ktlibrary.WritingApplication;
import ktlibrary.domain.ManuscriptEdited;
import ktlibrary.domain.ManuscriptRegistered;
import lombok.Data;

@Entity
@Table(name = "Manuscript_table")
@Data
//<<< DDD / Aggregate Root
public class Manuscript {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;

    private String content;

    @Embedded
    private AuthorId authorId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @PostPersist
    public void onPostPersist() {
        ManuscriptRegistered manuscriptRegistered = new ManuscriptRegistered(
            this
        );
        manuscriptRegistered.publishAfterCommit();
    }

    @PreUpdate
    public void onPreUpdate() {
        ManuscriptEdited manuscriptEdited = new ManuscriptEdited(this);
        manuscriptEdited.publishAfterCommit();
    }

    public static ManuscriptRepository repository() {
        ManuscriptRepository manuscriptRepository = WritingApplication.applicationContext.getBean(
            ManuscriptRepository.class
        );
        return manuscriptRepository;
    }

    //<<< Clean Arch / Port Method
    public void requestPublish(RequestPublishCommand requestPublishCommand) {
        //implement business logic here:

        PublishingRequested publishingRequested = new PublishingRequested(this);
        publishingRequested.publishAfterCommit();
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
