package com.tup.reconac.modules.vulnEnum.dtos.enrichment.nvd;

import java.util.List;

public record NvdReferenceData(
        String url,
        String source,
        List<String> tags
) {
}
