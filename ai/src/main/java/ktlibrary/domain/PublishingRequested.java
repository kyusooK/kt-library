package ktlibrary.domain;

import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

@Data
@ToString
public class PublishingRequested extends AbstractEvent {

    private Long id;
    private String title;
    private Object authorId;
    private Object status;
    private String content;
}
