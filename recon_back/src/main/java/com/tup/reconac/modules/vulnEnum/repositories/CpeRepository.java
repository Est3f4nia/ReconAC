package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.Cpe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CpeRepository extends JpaRepository<Cpe, UUID> {

    Optional<Cpe> findByUri(String uri);
    List<Cpe> findByIdIn(Collection<UUID> ids);
}
