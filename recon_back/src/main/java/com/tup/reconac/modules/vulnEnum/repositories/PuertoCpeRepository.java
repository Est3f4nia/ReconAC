package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.PuertoCpe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PuertoCpeRepository extends JpaRepository<PuertoCpe, UUID> {

    List<PuertoCpe> findByPuertoIdIn(Collection<UUID> puertoIds);

    @Modifying
    @Query("delete from PuertoCpe pc where pc.puertoId in :puertoIds")
    void deleteByPuertoIdIn(Collection<UUID> puertoIds);

    boolean existsByPuertoIdAndCpeId(UUID puertoId, UUID cpeId);
}
