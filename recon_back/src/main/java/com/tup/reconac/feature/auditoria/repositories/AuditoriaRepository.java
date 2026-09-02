package com.tup.reconac.feature.auditoria.repositories;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    List<Auditoria> findByUsuarioIdOrderByFechaGeneracionDesc(UUID usuarioId);
    Page<Auditoria> findByUsuarioId(UUID usuarioId, Pageable pageable);
}
