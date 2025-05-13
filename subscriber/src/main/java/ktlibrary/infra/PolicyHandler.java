package ktlibrary.infra;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.naming.NameParser;
import javax.naming.NameParser;
import javax.transaction.Transactional;
import ktlibrary.config.kafka.KafkaProcessor;
import ktlibrary.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

//<<< Clean Arch / Inbound Adaptor
@Service
@Transactional
public class PolicyHandler {

    @Autowired
    UserRepository userRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {}

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='OutOfPoint'"
    )
    public void wheneverOutOfPoint_FailSubscription(
        @Payload OutOfPoint outOfPoint
    ) {
        OutOfPoint event = outOfPoint;
        System.out.println(
            "\n\n##### listener FailSubscription : " + outOfPoint + "\n\n"
        );

        // Sample Logic //
        Subscription.failSubscription(event);
    }

    @StreamListener(
        value = KafkaProcessor.INPUT,
        condition = "headers['type']=='SubscriptionFailed'"
    )
    public void wheneverSubscriptionFailed_GuideFeeConversionSuggestion(
        @Payload SubscriptionFailed subscriptionFailed
    ) {
        SubscriptionFailed event = subscriptionFailed;
        System.out.println(
            "\n\n##### listener GuideFeeConversionSuggestion : " +
            subscriptionFailed +
            "\n\n"
        );

        // Sample Logic //
        User.guideFeeConversionSuggestion(event);
    }
}
//>>> Clean Arch / Inbound Adaptor
