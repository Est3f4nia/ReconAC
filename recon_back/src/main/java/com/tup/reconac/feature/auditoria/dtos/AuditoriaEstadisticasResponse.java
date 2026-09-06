package com.tup.reconac.feature.auditoria.dtos;

public record AuditoriaEstadisticasResponse(
        int totalEscaneos,
        int escaneosCompletados,
        int escaneosEnProceso,
        int escaneosPendientes,
        int escaneosFallidos
) { }
