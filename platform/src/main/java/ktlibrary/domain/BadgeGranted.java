package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class BadgeGranted extends AbstractEvent {

    private Long id;
    private String bookName;
    private Integer subscriptionCount;
    private Boolean isBestSeller;

    public BadgeGranted(Book aggregate) {
        super(aggregate);
    }

    public BadgeGranted() {
        super();
    }
}
//>>> DDD / Domain Event
