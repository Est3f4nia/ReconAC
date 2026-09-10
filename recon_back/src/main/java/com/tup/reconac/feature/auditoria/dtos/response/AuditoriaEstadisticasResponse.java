package com.tup.reconac.feature.auditoria.dtos.response;

public record AuditoriaEstadisticasResponse(
        int totalEscaneos,
        int escaneosCompletados,
        int escaneosEnProceso,
        int escaneosPendientes,
        int escaneosFallidos
) { }
