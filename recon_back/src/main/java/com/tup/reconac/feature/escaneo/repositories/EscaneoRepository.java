package com.tup.reconac.feature.escaneo.repositories;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {

    Optional<Escaneo> findByIdAndAuditoriaId(UUID id, UUID auditoriaId);
    Optional<Escaneo> findFirstByAuditoriaIdOrderByCreadoADesc(UUID auditoriaId);

    Optional<Escaneo> findByModuloJobId(String jobId);

    Page<Escaneo> findByAuditoriaIdIn(
            List<UUID> auditoriaIds,
            Pageable pageable
    );

    // Estadísticas

    List<Escaneo> findByAuditoriaIdOrderByCreadoADesc(UUID auditoriaId);
}
