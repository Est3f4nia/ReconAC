package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.CveCwe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CveCweRepository extends JpaRepository<CveCwe, UUID> {

    List<CveCwe> findByCveId(UUID cveId);

    List<CveCwe> findByCweId(UUID cweId);

    Optional<CveCwe> findByCveIdAndCweId(UUID cveId, UUID cweId);
}
