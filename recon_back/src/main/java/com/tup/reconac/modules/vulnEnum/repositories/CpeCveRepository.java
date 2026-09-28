package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CpeCveRepository extends JpaRepository<CpeCve, UUID> {

    List<CpeCve> findByCpeId(UUID cpeId);

    List<CpeCve> findByCpeIdIn(Collection<UUID> cveIds);

    Optional<CpeCve> findByCpeIdAndCveId(UUID cpeId, UUID cveI);
}
