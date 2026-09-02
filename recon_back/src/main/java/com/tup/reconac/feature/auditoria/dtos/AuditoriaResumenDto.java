package com.tup.reconac.feature.auditoria.dtos;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaResumenDto(
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
