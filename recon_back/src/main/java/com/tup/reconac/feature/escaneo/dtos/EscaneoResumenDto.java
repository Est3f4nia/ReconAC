package com.tup.reconac.feature.escaneo.dtos;

import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.UUID;

public record EscaneoResumenDto(
        UUID escaneoId,
        UUID auditoriaId,
        String auditoriaNombre,
        Integer activos,
        Integer puertos,
        LocalDateTime ultimoEscaneo,
        EscaneoEstado status,
        Integer cve,
        Integer cveCriticos
) {}
