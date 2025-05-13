package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import ktlibrary.ReviewApplication;
import ktlibrary.domain.ReviewDeleted;
import ktlibrary.domain.ReviewEdited;
import ktlibrary.domain.ReviewRegistered;
import lombok.Data;

@Entity
@Table(name = "Review_table")
@Data
//<<< DDD / Aggregate Root
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    private BookId bookId;

    @Embedded
    private UserId userId;

    private String content;

    @PostPersist
    public void onPostPersist() {
        ReviewRegistered reviewRegistered = new ReviewRegistered(this);
        reviewRegistered.publishAfterCommit();
    }

    @PreUpdate
    public void onPreUpdate() {
        ReviewEdited reviewEdited = new ReviewEdited(this);
        reviewEdited.publishAfterCommit();
    }

    @PreRemove
    public void onPreRemove() {
        ReviewDeleted reviewDeleted = new ReviewDeleted(this);
        reviewDeleted.publishAfterCommit();
    }

    public static ReviewRepository repository() {
        ReviewRepository reviewRepository = ReviewApplication.applicationContext.getBean(
            ReviewRepository.class
        );
        return reviewRepository;
    }
}
//>>> DDD / Aggregate Root
