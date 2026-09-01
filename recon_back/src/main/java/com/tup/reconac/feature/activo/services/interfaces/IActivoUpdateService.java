package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;

import java.util.UUID;

public interface IActivoUpdateService {
    ActivoResponse update(ActivoRequestDto req, UUID id);
}
