package org.ria.ifzz.RiaApp.repositorie;

import org.ria.ifzz.RiaApp.domain.Backlog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacklogRepository extends CrudRepository<Backlog, Long> {

    @Override
    Iterable<Backlog> findAll();

    Backlog getById(Long id);

    Backlog findByDataId(String id);
}
