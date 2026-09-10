package com.tup.reconac.feature.puerto.repositories;

import com.tup.reconac.feature.puerto.models.Puerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PuertoRepository extends JpaRepository<Puerto, UUID> {

    List<Puerto> findByActivoId(UUID activoId);

    List<Puerto> findByActivoIdIn(Collection<UUID> activoIds);

    void deleteByActivoIdIn(Collection<UUID> activoIds);
}
