package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.Cwe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CweRepository extends JpaRepository<Cwe, UUID> {

    Optional<Cwe> findByCweCode(String cweCode);
}
