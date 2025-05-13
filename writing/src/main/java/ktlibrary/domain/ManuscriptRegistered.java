package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class ManuscriptRegistered extends AbstractEvent {

    private Long id;
    private String title;
    private String content;
    private AuthorId authorId;
    private Status status;

    public ManuscriptRegistered(Manuscript aggregate) {
        super(aggregate);
    }

    public ManuscriptRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
