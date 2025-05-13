package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class AuthorRegistered extends AbstractEvent {

    private Long id;
    private String email;
    private String authorName;
    private String introduction;
    private String feturedWorks;
    private List<Portfolio> portfolios;
    private Boolean isApprove;

    public AuthorRegistered(Author aggregate) {
        super(aggregate);
    }

    public AuthorRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
