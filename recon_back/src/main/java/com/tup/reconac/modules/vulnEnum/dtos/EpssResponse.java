package com.tup.reconac.modules.vulnEnum.dtos;

import com.tup.reconac.modules.vulnEnum.dtos.data.EpssData;

import java.util.List;

public record EpssResponse(
        String status,
        Integer total,
        List<EpssData> data
) {
}
