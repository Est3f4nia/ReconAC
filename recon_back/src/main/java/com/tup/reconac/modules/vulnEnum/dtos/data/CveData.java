package com.tup.reconac.modules.vulnEnum.dtos.data;

import java.math.BigDecimal;
import java.util.List;

public record CveData(
        String id,
        BigDecimal cvss,
        BigDecimal epss,
        boolean explotacionActiva,
        List<String> cwes
) {
}
