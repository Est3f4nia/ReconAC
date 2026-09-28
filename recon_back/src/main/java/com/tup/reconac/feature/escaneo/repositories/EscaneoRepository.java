package com.tup.reconac.feature.escaneo.repositories;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.*;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {

    Optional<Escaneo> findByIdAndAuditoriaId(UUID escaneoId, UUID auditoriaId);

    Optional<Escaneo> findFirstByAuditoriaIdOrderByCreadoADesc(UUID auditoriaId);

    Optional<Escaneo> findByModuloJobId(String jobId);

    Page<Escaneo> findByAuditoriaIdIn(List<UUID> auditoriaIds, Pageable pageable);

    // Estadísticas
    List<Escaneo> findByAuditoriaIdOrderByCreadoADesc(UUID auditoriaId);

    // Obtiene los ID de todos los escaneos relacionados con una auditoría
    @Query("select e.id from Escaneo e where e.auditoriaId in :auditoriaIds")
    List<UUID> findIdsByAuditoriaIdIn(Collection<UUID> auditoriaIds);

    @Query("select e.id from Escaneo e where e.auditoriaId = :auditoriaId")
    List<UUID> findIdsByAuditoriaId(UUID auditoriaId);
}
