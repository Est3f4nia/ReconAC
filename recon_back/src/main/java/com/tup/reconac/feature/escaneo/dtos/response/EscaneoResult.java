package com.tup.reconac.feature.escaneo.dtos.response;

import com.tup.reconac.feature.activo.dtos.response.ActivoResultadoResponse;
import com.tup.reconac.feature.escaneo.models.EscaneoEstado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ?????????????????
public record EscaneoResult(
        List<HostResult> hosts,
        Map<String, Object> apiResults,
        String nmapVersion,
        String startTime,
        String endTime
) {
    public static record EscaneoResultResponse(
            UUID escaneoId,
            EscaneoEstado estado,
            Integer progreso,
            String nmapVersion,
            LocalDateTime iniciadoA,
            LocalDateTime completadoA,
            List<ActivoResultadoResponse> activos
    ) {}
}
