package com.tup.reconac.modules.vulnEnum.dtos.data;

import java.math.BigDecimal;

public record EpssData(
        String cve,
        BigDecimal epss,
        BigDecimal percentile
) {
}
