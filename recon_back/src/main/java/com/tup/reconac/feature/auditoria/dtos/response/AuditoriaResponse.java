package com.tup.reconac.feature.auditoria.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaResponse(
        UUID id,
        String nombre,
        String objetivo,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaFinal
) {}
