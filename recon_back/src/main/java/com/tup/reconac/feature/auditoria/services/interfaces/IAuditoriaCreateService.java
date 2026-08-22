package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.AuditoriaResponse;

public interface IAuditoriaCreateService {
    AuditoriaResponse create(AuditoriaRequestDto req);
}
