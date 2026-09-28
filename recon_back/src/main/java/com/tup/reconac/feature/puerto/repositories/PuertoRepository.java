package com.tup.reconac.feature.puerto.repositories;

import com.tup.reconac.feature.puerto.models.Puerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PuertoRepository extends JpaRepository<Puerto, UUID> {

    List<Puerto> findByActivoIdIn(Collection<UUID> activoIds);

    // PuertoRepository
    @Query("select p.id from Puerto p where p.activoId in :activoIds")
    List<UUID> findIdsByActivoIdIn(Collection<UUID> activoIds);

    @Modifying
    @Query("delete from Puerto p where p.activoId in :activoIds")
    void deleteByActivoIds(Collection<UUID> activoIds);
}
