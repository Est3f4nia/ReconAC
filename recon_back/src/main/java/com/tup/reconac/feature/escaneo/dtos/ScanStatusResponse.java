package com.tup.reconac.feature.escaneo.dtos;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

public record ScanStatusResponse(
        String scanId,
        EscaneoEstado status,
        Integer progress,
        String error
) {}
