package com.tup.reconac.feature.escaneo.repositories;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {

    Optional<Escaneo> findByModuloJobId(String moduloJobId);

    @Query(value = """
            SELECT e.* FROM escaneo e
            INNER JOIN auditoria a ON e.auditoria_id = a.auditoria_id
            WHERE a.usuario_id = :usuarioId
            ORDER BY e.creado_a DESC
            """, nativeQuery = true)
    List<Escaneo> findResumenByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
