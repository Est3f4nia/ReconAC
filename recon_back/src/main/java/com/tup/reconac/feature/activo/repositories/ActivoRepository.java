package com.tup.reconac.feature.activo.repositories;

import com.tup.reconac.feature.activo.models.Activo;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivoRepository extends JpaRepository<Activo, UUID> {

    Page<Activo> findByEscaneoId(UUID escaneoId, Pageable pageable);
}
