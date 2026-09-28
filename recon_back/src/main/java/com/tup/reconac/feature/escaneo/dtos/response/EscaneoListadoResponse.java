package com.tup.reconac.feature.escaneo.dtos.response;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response del listado de escaneos
 */

// puertosssssssss y ¿resultado json?
public record EscaneoListadoResponse(
        UUID escaneoId,
        UUID auditoriaId,
        String auditoriaNombre,
        List<String> objetivos,
        EscaneoEstado estado,
        Integer progreso,
        String nmapVersion,
        LocalDateTime iniciadoA,
        LocalDateTime completadoA
) {}