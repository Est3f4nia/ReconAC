package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.request.ActivoRequestDto;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;

public interface IActivoCreateService {
    ActivoResponse create(ActivoRequestDto req);
}
