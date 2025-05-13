package ktlibrary.domain;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import ktlibrary.WritingApplication;
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
        ManuscriptRegistered manuscriptRegistered = new ManuscriptRegistered(this);
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
        // 원고 조회
        repository().findById(this.getId()).ifPresent(manuscript ->{
            // 원고의 상태를 완료처리 후 이벤트 발행
            if(requestPublishCommand.getStatus() == status.DONE){
                this.setStatus(requestPublishCommand.getStatus());

                PublishingRequested publishingRequested = new PublishingRequested(this);
                publishingRequested.publishAfterCommit();
            }
        });

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
