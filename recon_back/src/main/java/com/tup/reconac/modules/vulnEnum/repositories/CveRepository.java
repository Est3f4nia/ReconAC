package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.Cve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CveRepository extends JpaRepository<Cve, UUID> {

    Optional<Cve> findByCve(String cve);
}
