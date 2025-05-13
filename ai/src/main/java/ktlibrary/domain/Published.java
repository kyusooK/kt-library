package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

//<<< DDD / Domain Event
@Data
@ToString
public class Published extends AbstractEvent {

    private Long id;
    private String manuscript;
    private String image;
    private String summaryContent;
    private String bookName;
    private ManuscriptId manuscriptId;
    private String bookPdf;
    private Long authorId;

    public Published(Publishing aggregate) {
        super(aggregate);
    }

    public Published() {
        super();
    }
}
//>>> DDD / Domain Event
