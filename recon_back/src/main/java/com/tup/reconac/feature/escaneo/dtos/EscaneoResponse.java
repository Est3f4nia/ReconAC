package com.tup.reconac.feature.escaneo.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record EscaneoResponse(
        UUID id,
        UUID auditoriaId,
        String[] objetivos,
        String estado,
        Integer progreso,
        String nmapVersion,
        String mensajeError,
        LocalDateTime iniciadoA,
        LocalDateTime completadoA,
        LocalDateTime creadoA
) {}
