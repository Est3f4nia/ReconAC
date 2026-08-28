package com.tup.reconac.feature.escaneo.dtos;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.UUID;

public record EscaneoResponse(
        UUID id,
        UUID auditoriaId,
        String[] objetivos,
        EscaneoEstado estado,
        Integer progreso,
        String nmapVersion,
        String mensajeError,
        LocalDateTime iniciadoA,
        LocalDateTime completadoA,
        LocalDateTime creadoA
) {}
