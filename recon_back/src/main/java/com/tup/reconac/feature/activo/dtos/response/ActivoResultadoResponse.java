package com.tup.reconac.feature.activo.dtos.response;

import com.tup.reconac.feature.puerto.dtos.PuertoResultadoResponse;

import java.util.List;
import java.util.UUID;

// Resultado COMPLETO del recon de un activo
public record ActivoResultadoResponse(
        UUID activoId,
        String host,
        String hostname,
        String so,
        Integer soProbab,
        String mac,
        List<PuertoResultadoResponse> puertos
) {}
