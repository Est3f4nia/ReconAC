package com.tup.reconac.feature.escaneo.dtos.internal;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.util.UUID;

public record ScanStatusResponse(
        UUID scanId,
        EscaneoEstado status,
        Integer progress,
        String error
) {}
