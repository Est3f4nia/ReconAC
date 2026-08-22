package com.tup.reconac.feature.auditoria.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AuditoriaRequestDto(
        // NOTA: provisional hasta la implementación de JWT
        @NotNull
        @Positive
        UUID usuarioId,

        @Size(max = 50)
        String nombre,

        @Size(max = 150)
        String objetivo
) {}
