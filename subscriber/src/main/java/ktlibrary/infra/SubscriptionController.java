package ktlibrary.infra;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import ktlibrary.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//<<< Clean Arch / Inbound Adaptor

@RestController
// @RequestMapping(value="/subscriptions")
@Transactional
public class SubscriptionController {

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @RequestMapping(
        value = "/subscriptions/{id}/cancelsubscription",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public Subscription cancelSubscription(
        @PathVariable(value = "id") Long id,
        @RequestBody CancelSubscriptionCommand cancelSubscriptionCommand,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        System.out.println(
            "##### /subscription/cancelSubscription  called #####"
        );
        Optional<Subscription> optionalSubscription = subscriptionRepository.findById(
            id
        );

        optionalSubscription.orElseThrow(() -> new Exception("No Entity Found")
        );
        Subscription subscription = optionalSubscription.get();
        subscription.cancelSubscription(cancelSubscriptionCommand);

        subscriptionRepository.save(subscription);
        return subscription;
    }
}
//>>> Clean Arch / Inbound Adaptor
