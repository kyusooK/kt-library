package ktlibrary.domain;

import ktlibrary.domain.*;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

//<<< PoEAA / Repository
@RepositoryRestResource(
    collectionResourceRel = "publishings",
    path = "publishings"
)
public interface PublishingRepository
    extends PagingAndSortingRepository<Publishing, Long> {}
