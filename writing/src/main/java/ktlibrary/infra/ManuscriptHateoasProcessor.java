package ktlibrary.infra;

import ktlibrary.domain.*;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class ManuscriptHateoasProcessor
    implements RepresentationModelProcessor<EntityModel<Manuscript>> {

    @Override
    public EntityModel<Manuscript> process(EntityModel<Manuscript> model) {
        model.add(
            Link
                .of(model.getRequiredLink("self").getHref() + "/requestpublish")
                .withRel("requestpublish")
        );

        return model;
    }
}
