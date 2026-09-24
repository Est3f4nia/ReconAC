package com.tup.reconac.feature.escaneo.dtos.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.util.UUID;

public record ScanStatusResponse(
        @Schema(description = "En GET status es el UUID persistido del escaneo; en el callback de progreso Python envía el UUID del trabajo.")
        UUID scanId,

        EscaneoEstado status,

        @Schema(description = "Porcentaje de progreso (0 a 100).")
        Integer progress,

        String error
) {}
