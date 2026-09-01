package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.ActivoResponse;

public interface IActivoCreateService {
    ActivoResponse create(ActivoRequestDto req);
}
