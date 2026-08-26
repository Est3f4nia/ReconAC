package com.tup.reconac.feature.escaneo.repositories;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {
}
