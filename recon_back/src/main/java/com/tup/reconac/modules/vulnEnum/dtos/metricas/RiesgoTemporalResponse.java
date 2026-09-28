package com.tup.reconac.modules.vulnEnum.dtos.metricas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RiesgoTemporalResponse(
        UUID escaneoId,
        LocalDateTime fecha,
        String nivel,
        BigDecimal cvssPromedio,
        long cves,
        long cvesCriticos,
        long cvesExplotados
) {}