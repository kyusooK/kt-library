package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class PublishingRequested extends AbstractEvent {

    private Long id;
    private String title;
    private AuthorId authorId;
    private Status status;
    private String content;

    public PublishingRequested(Manuscript aggregate) {
        super(aggregate);
    }

    public PublishingRequested() {
        super();
    }
}
//>>> DDD / Domain Event
