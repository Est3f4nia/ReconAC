package com.tup.reconac.modules.vulnEnum.dtos.enrichment.nvd;

import java.time.LocalDateTime;
import java.util.List;

public record NvdCacheEntry(
        String cpe,
        LocalDateTime lastChecked,
        List<NvdVulnerabilityData> vulnerabilities
) {
}
