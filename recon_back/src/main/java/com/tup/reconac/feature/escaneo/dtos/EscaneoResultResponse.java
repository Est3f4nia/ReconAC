package com.tup.reconac.feature.escaneo.dtos;

public record EscaneoResultResponse(
        EscaneoResponse escaneo,
        String resultado
) {}
