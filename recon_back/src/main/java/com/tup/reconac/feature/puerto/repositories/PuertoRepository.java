package com.tup.reconac.feature.puerto.repositories;

import com.tup.reconac.feature.puerto.models.Puerto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PuertoRepository extends JpaRepository<Puerto, UUID> {

    List<Puerto> findByActivoIdIn(List<UUID> activoIds);
}
