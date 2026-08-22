package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.ActivoResponse;

import java.util.List;

public interface IActivoGetService {
    List<ActivoResponse> getAll();
}
