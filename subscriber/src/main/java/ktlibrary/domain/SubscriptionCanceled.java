package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class SubscriptionCanceled extends AbstractEvent {

    private Long id;
    private Boolean isSubscription;

    public SubscriptionCanceled(Subscription aggregate) {
        super(aggregate);
    }

    public SubscriptionCanceled() {
        super();
    }
}
//>>> DDD / Domain Event
