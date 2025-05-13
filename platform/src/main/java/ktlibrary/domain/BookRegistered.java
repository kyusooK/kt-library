package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class BookRegistered extends AbstractEvent {

    private Long id;
    private String bookName;
    private String category;
    private Boolean isBestSeller;
    private String image;
    private String summaryContent;
    private String bookContent;
    private String authorName;

    public BookRegistered(Book aggregate) {
        super(aggregate);
    }

    public BookRegistered() {
        super();
    }
}
//>>> DDD / Domain Event
