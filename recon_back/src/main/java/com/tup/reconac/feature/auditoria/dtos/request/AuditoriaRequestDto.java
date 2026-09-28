package com.tup.reconac.feature.auditoria.dtos.request;

import jakarta.validation.constraints.Size;

public record AuditoriaRequestDto(
        @Size(max = 50)
        String nombre,

        @Size(max = 150)
        String objetivo
) {}
