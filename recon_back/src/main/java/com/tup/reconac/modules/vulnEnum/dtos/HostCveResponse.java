package com.tup.reconac.modules.vulnEnum.dtos;

import java.math.BigDecimal;
import java.util.List;

public record HostCveResponse(
        String cveId,
        BigDecimal cvssScore,
        BigDecimal epssScore,
        boolean kev,
        List<String> cpes
) {
}
