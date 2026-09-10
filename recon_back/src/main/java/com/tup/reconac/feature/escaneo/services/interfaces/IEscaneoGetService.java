package com.tup.reconac.feature.escaneo.services.interfaces;

import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;

import java.util.UUID;

public interface IEscaneoGetService {
    ScanStatusResponse getStatus(UUID auditoriaId, UUID escaneoId);

    EscaneoResult.EscaneoResultResponse getResultado(UUID auditoriaId, UUID escaneoId);

    EscaneoResult.EscaneoResultResponse getById(UUID id);
}
