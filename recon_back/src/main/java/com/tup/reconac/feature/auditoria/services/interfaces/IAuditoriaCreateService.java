package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;

public interface IAuditoriaCreateService {
    AuditoriaResponse create(AuditoriaRequestDto req);
}
