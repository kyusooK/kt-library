package ktlibrary.domain;

import java.util.*;
import ktlibrary.domain.*;
import ktlibrary.infra.AbstractEvent;
import lombok.*;

@Data
@ToString
public class UserRegistered extends AbstractEvent {

    private Long id;
    private String email;
    private String userName;
}
