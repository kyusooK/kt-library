package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class AuthorDisApproved extends AbstractEvent {

    private Boolean isApprove;

    public AuthorDisApproved(Author aggregate) {
        super(aggregate);
    }

    public AuthorDisApproved() {
        super();
    }
}
//>>> DDD / Domain Event
