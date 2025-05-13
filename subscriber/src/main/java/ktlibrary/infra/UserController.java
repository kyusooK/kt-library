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
// @RequestMapping(value="/users")
@Transactional
public class UserController {

    @Autowired
    UserRepository userRepository;

    @RequestMapping(
        value = "/users/{id}/buysubscription",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public User buySubscription(
        @PathVariable(value = "id") Long id,
        @RequestBody BuySubscriptionCommand buySubscriptionCommand,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        System.out.println("##### /user/buySubscription  called #####");
        Optional<User> optionalUser = userRepository.findById(id);

        optionalUser.orElseThrow(() -> new Exception("No Entity Found"));
        User user = optionalUser.get();
        user.buySubscription(buySubscriptionCommand);

        userRepository.save(user);
        return user;
    }
}
//>>> Clean Arch / Inbound Adaptor
