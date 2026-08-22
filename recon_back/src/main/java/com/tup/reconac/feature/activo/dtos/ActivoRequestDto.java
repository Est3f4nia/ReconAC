package com.tup.reconac.feature.activo.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ActivoRequestDto(
        @NotNull
        UUID auditoriaId,

        @NotNull
        String host,

        String hostname,

        String so,

        Integer soProbab,

        String mac,

        String descripcion
) {}
