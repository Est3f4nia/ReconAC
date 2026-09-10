package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.Referencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenciaRepository extends JpaRepository<Referencia, UUID> {

    List<Referencia> findByCveId(UUID cveId);

    Optional<Referencia> findByCveIdAndUrl(UUID cveId, String url);
}
