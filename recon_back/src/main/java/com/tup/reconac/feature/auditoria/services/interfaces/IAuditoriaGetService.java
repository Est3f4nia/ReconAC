package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaEstadisticasResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IAuditoriaGetService {
    Page<AuditoriaResponse> getAll(Pageable pageable);
    AuditoriaResponse getById(UUID id);
    AuditoriaEstadisticasResponse getEstadisticas(UUID id);
}
