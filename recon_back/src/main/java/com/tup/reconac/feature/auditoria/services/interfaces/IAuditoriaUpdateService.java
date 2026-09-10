package com.tup.reconac.feature.auditoria.services.interfaces;

import com.tup.reconac.feature.auditoria.dtos.request.AuditoriaRequestDto;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;

import java.util.UUID;

public interface IAuditoriaUpdateService {
    AuditoriaResponse update(AuditoriaRequestDto req, UUID id);
}
