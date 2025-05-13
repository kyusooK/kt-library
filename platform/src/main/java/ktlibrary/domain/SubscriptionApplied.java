package ktlibrary.domain;

import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

@Data
@ToString
public class SubscriptionApplied extends AbstractEvent {

    private Long id;
    private Object bookId;
    private Object userId;
    private Boolean isSubscription;
    private Date startSubscription;
    private Date endSubscription;
    private String pdfPath;
}
