package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class ReviewRegistered extends AbstractEvent {

    private Long id;
    private BookId bookId;
    private UserId userId;
    private String content;

    public ReviewRegistered(Review aggregate) {
        super(aggregate);
    }

    public ReviewRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
