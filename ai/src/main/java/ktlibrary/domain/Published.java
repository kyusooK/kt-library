package ktlibrary.domain;

import ktlibrary.infra.AbstractEvent;
import lombok.Data;
import lombok.ToString;

//<<< DDD / Domain Event
@Data
@ToString
public class Published extends AbstractEvent {

    private Long id;
    private String image;
    private String summaryContent;
    private String bookName;
    private String pdfPath;
    private String webUrl;
    private String authorId;
    private String category;

    public Published(Publishing aggregate) {
        super(aggregate);
    }

    public Published() {
        super();
    }
}
//>>> DDD / Domain Event
