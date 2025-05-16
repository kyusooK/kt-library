package ktlibrary.domain;

import ktlibrary.infra.AbstractEvent;
import lombok.Data;
import lombok.ToString;
import javax.persistence.Lob;

//<<< DDD / Domain Event
@Data
@ToString
public class Published extends AbstractEvent {

    private Long id;
    
    @Lob
    private String image;
    
    @Lob
    private String summaryContent;
    
    private String bookName;
    
    @Lob
    private String pdfPath;
    
    @Lob
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
