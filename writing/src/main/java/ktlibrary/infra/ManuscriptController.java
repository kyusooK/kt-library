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
// @RequestMapping(value="/manuscripts")
@Transactional
public class ManuscriptController {

    @Autowired
    ManuscriptRepository manuscriptRepository;

    @RequestMapping(
        value = "/manuscripts/{id}/requestpublish",
        method = RequestMethod.PUT,
        produces = "application/json;charset=UTF-8"
    )
    public Manuscript requestPublish(
        @PathVariable(value = "id") Long id,
        @RequestBody RequestPublishCommand requestPublishCommand,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws Exception {
        System.out.println("##### /manuscript/requestPublish  called #####");
        Optional<Manuscript> optionalManuscript = manuscriptRepository.findById(
            id
        );

        optionalManuscript.orElseThrow(() -> new Exception("No Entity Found"));
        Manuscript manuscript = optionalManuscript.get();
        manuscript.requestPublish(requestPublishCommand);

        manuscriptRepository.save(manuscript);
        return manuscript;
    }
}
//>>> Clean Arch / Inbound Adaptor
