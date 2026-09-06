package com.tup.reconac.modules.vulnEnum.dtos;

import java.util.List;

public record DashboardAuditoriaResponse(
        DashboardKpisResponse kpis,
        List<EjecucionHistorialResponse> historial,
        List<RiesgoTemporalResponse> riesgoTemporal,
        CveDesgloseResponse vulnerabilidades,
        HostDesgloseResponse hosts
) {}
