package ktlibrary.domain;

import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

@Data
@ToString
public class Published extends AbstractEvent {

    private Long id;
    private String manuscript;
    private String image;
    private String summaryContent;
    private String bookName;
    private Object manuscriptId;
    private String bookPdf;
    private Long authorId;
}
