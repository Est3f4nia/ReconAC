package com.tup.reconac.modules.vulnEnum.dtos;

import java.math.BigDecimal;

public record DashboardKpisResponse(
        long escaneos,
        long activos,
        long puertos,
        long cves,
        long cvesCriticos,
        long cvesAltos,
        long cvesMedios,
        long cvesBajos,
        BigDecimal cvssPromedio,
        long cvesExplotados,
        BigDecimal epssPromedio       // null mientras el dato no pueda obtenerse correctamente
) {}
