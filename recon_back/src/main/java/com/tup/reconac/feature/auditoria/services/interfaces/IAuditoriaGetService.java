package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAuditoriaGetService {
    Page<AuditoriaResponse> getAll(Pageable pageable);
}
