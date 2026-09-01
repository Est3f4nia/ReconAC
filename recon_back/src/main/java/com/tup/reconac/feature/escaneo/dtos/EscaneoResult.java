package com.tup.reconac.feature.escaneo.dtos;

import java.util.List;
import java.util.Map;

public record EscaneoResult(
        List<HostResult> hosts,
        Map<String, Object> apiResults,
        String nmapVersion,
        String startTime,
        String endTime
) {}
