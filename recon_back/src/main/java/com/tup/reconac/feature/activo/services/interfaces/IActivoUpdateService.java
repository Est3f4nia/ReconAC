package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;

import java.util.UUID;

public interface IActivoUpdateService {
    ActivoResponse update(ActivoRequestDto req, UUID id);
}
