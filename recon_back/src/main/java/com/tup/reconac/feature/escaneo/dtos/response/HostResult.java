package com.tup.reconac.feature.escaneo.dtos.response;

import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;

import java.util.List;

public record HostResult(
        String ip,
        String mac,
        String hostname,
        String os,
        Integer soProbab,
        List<PuertoResultadoResponse> puertos
) {}
