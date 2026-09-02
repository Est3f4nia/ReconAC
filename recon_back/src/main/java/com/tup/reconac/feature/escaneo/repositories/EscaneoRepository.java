package com.tup.reconac.feature.escaneo.repositories;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {

    Optional<Escaneo> findByModuloJobId(String moduloJobId);

    @Query("SELECT e FROM Escaneo e WHERE e.auditoriaId = :auditoriaId ORDER BY e.creadoA DESC")
    Optional<Escaneo> findFirstByAuditoriaIdOrderByCreadoADesc(@Param("auditoriaId") UUID auditoriaId);
}
