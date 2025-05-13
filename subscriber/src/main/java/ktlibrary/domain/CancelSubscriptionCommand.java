package ktlibrary.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.Data;

@Data
public class CancelSubscriptionCommand {

    private Long id;
    private Boolean isSubscription;
}
