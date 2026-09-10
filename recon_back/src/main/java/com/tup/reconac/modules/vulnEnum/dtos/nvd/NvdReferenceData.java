package com.tup.reconac.modules.vulnEnum.dtos.nvd;

import java.util.List;

public record NvdReferenceData(
        String url,
        String source,
        List<String> tags
) {
}
