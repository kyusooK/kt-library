package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class ReviewDeleted extends AbstractEvent {

    private Long id;
    private BookId bookId;
    private UserId userId;
    private String content;

    public ReviewDeleted(Review aggregate) {
        super(aggregate);
    }

    public ReviewDeleted() {
        super();
    }
}
//>>> DDD / Domain Event
