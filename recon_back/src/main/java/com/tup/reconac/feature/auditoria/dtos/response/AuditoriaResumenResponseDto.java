package com.tup.reconac.feature.auditoria.dtos.response;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO cards auditoría
 */

public record AuditoriaResumenResponseDto(
        UUID escaneoId,
        UUID auditoriaId,
        String auditoriaNombre,
        Integer activos,
        Integer puertos,
        LocalDateTime completadoA,
        EscaneoEstado status,
        Integer cve,
        Integer cveCriticos
) {}
