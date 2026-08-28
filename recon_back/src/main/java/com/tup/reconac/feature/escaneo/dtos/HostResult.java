package com.tup.reconac.feature.escaneo.dtos;

public record HostResult(
        String ip,
        String mac,
        String hostname,
        String os
) {}
