package com.tup.reconac.modules.vulnEnum.repositories;

import com.tup.reconac.modules.vulnEnum.models.CpeCve;
import com.tup.reconac.modules.vulnEnum.models.Cve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CveRepository extends JpaRepository<Cve, UUID> {

    Optional<Cve> findByCveIgnoreCase(String cve);

    Optional<Cve> findByCve(String cve);

    List<Cve> findAllByCveIn(Collection<String> cves);

    long countByCveInAndKevTrue(Collection<String> cves);

}
