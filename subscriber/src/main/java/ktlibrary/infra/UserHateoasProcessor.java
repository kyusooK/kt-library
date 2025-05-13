package ktlibrary.infra;

import ktlibrary.domain.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class UserHateoasProcessor
    implements RepresentationModelProcessor<EntityModel<User>> {

    @Override
    public EntityModel<User> process(EntityModel<User> model) {
        model.add(
            Link
                .of(
                    model.getRequiredLink("self").getHref() + "/buysubscription"
                )
                .withRel("buysubscription")
        );

        return model;
    }
}
