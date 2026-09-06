package com.tup.reconac.modules.vulnEnum.dtos;

import com.tup.reconac.modules.vulnEnum.dtos.data.KevData;

import java.util.List;

public record KevResponse(
        String title,
        String catalogVersion,
        String dateReleased,
        int count,
        List<KevData> vulnerabilities
) {
}
