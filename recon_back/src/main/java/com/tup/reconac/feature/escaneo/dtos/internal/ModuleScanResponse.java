package com.tup.reconac.feature.escaneo.dtos.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModuleScanResponse(
        @JsonProperty("scan_id")
        String scanId
) {}
