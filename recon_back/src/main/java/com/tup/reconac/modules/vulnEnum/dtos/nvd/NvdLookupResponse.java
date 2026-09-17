package com.tup.reconac.modules.vulnEnum.dtos.nvd;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record NvdLookupResponse(
        @Schema(description = "Resultados indexados por cadena CPE; pueden omitirse CPEs no consultables.")
        Map<String, NvdCacheEntry> results
) {
}
