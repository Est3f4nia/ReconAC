package com.tup.reconac.modules.vulnEnum.dtos;

import java.math.BigDecimal;
import java.util.List;

public record CveResumenResponse(
        String cveId,
        long frecuencia,
        BigDecimal cvssScore,
        BigDecimal epssScore,
        boolean explotacionActiva,
        List<String> cwes
) {}
