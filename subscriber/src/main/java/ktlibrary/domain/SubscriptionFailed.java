package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class SubscriptionFailed extends AbstractEvent {

    private Long id;
    private Boolean isSubscription;
    private BookId bookId;
    private UserId userId;
    private Date startSubscription;
    private Date endSubscription;

    public SubscriptionFailed(Subscription aggregate) {
        super(aggregate);
    }

    public SubscriptionFailed() {
        super();
    }
}
//>>> DDD / Domain Event
