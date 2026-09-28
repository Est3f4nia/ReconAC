package com.tup.reconac.feature.activo.services.interfaces;

import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IActivoGetService {
    Page<ActivoAgrupadoResponse> getAll(Pageable pageable);
    Page<ActivoResponse> getByEscaneoId(UUID escaneoId, Pageable pageable);
}
