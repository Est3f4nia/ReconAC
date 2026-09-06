package com.tup.reconac.modules.vulnEnum.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EjecucionHistorialResponse(
        UUID escaneoId,
        LocalDateTime fecha,
        String estado,
        Integer progreso,
        List<String> objetivos,
        long activos,
        long puertos,
        long cves,
        long cvesCriticos,
        BigDecimal cvssPromedio
) {}
