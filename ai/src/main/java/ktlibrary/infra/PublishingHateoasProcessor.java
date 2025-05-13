package ktlibrary.infra;

import ktlibrary.domain.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class PublishingHateoasProcessor
    implements RepresentationModelProcessor<EntityModel<Publishing>> {

    @Override
    public EntityModel<Publishing> process(EntityModel<Publishing> model) {
        return model;
    }
}
