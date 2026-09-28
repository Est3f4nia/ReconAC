package com.tup.reconac.feature.auditoria.repositories;

import com.tup.reconac.feature.auditoria.models.Auditoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    Page<Auditoria> findByUsuarioIdOrderByFechaGeneracionDesc(UUID usuarioId, Pageable pageable);

    List<Auditoria> findByUsuarioIdOrderByFechaGeneracionDesc(UUID usuarioId);

    Optional<Auditoria> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuarioId(UUID id, UUID usuarioId);
}
