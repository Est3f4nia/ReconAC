package com.tup.reconac.feature.escaneo.services.interfaces;

import com.tup.reconac.feature.escaneo.dtos.EscaneoResultResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;

import java.util.UUID;

public interface IEscaneoGetService {
    ScanStatusResponse getStatus(UUID auditoriaId, UUID escaneoId);

    EscaneoResultResponse getResultado(UUID auditoriaId, UUID escaneoId);
}
