package com.tup.reconac.modules.vulnEnum.dtos.nvd;

import java.util.Map;

public record NvdLookupResponse(
        Map<String, NvdCacheEntry> results
) {
}
