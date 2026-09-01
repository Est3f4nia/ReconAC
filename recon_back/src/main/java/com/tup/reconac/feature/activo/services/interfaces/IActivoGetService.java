package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IActivoGetService {
    Page<ActivoResponse> getAll(Pageable pageable);
    Page<ActivoResponse> getByEscaneoId(UUID escaneoId, Pageable pageable);
}
