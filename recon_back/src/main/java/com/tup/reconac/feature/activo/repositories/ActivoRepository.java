package com.tup.reconac.feature.activo.repositories;

import com.tup.reconac.feature.activo.models.Activo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {

    Page<Activo> findByEscaneoId(UUID escaneoId, Pageable pageable);

    Optional<Activo> findByIdAndEscaneoId(UUID id, UUID escaneoId);

    List<Activo> findByEscaneoIdIn(List<UUID> escaneoIds);
}
