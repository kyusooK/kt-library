package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class PointBought extends AbstractEvent {

    private Long id;
    private Integer point;
    private UserId userId;

    public PointBought(Point aggregate) {
        super(aggregate);
    }

    public PointBought() {
        super();
    }
}
//>>> DDD / Domain Event
