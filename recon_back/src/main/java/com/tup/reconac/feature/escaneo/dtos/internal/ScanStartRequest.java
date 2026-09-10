package com.tup.reconac.feature.escaneo.dtos.internal;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScanStartRequest(
        @NotEmpty(message = "objetivos no puede estar vacío")
        List<String> objetivos,

        Integer timeout,

        Integer icmpTimeout,

        Integer maxCveYears,

        Double minCvssScore,

        String nvdApiKey
) {}
