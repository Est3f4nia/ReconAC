package com.tup.reconac.modules.vulnEnum.dtos.nvd;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NvdLookupRequest(
        UUID escaneoId,
        List<String> cpes,
        int maxCveYears,
        BigDecimal minCvssScore
) {
}
