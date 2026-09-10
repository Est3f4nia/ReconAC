package com.tup.reconac.feature.escaneo.dtos.response;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.UUID;

public record EscaneoResponse(
        UUID escaneoId,
        String[] activos,
        EscaneoEstado estado,
        Integer progreso,
        String nmapVersion,
        String mensajeError,
        LocalDateTime iniciadoA,
        LocalDateTime completadoA,
        LocalDateTime creadoA
) {}
