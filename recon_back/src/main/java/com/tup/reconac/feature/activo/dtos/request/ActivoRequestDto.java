package com.tup.reconac.feature.activo.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ActivoRequestDto(
        @NotNull
        UUID escaneoId,

        @NotNull
        String host,

        String hostname,

        String so,

        @Min(value = 0, message = "soProbab debe ser mayor o igual a 0")
        @Max(value = 100, message = "soProbab debe ser menor o igual a 100")
        Integer soProbab,

        String mac,

        String descripcion
) {}
