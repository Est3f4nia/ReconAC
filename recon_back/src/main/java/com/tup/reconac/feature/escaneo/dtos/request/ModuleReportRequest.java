package com.tup.reconac.feature.escaneo.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

public record ModuleReportRequest(
        String formato,

        @JsonProperty("scan_id")
        String scanId,

        JsonNode resultado
) {}
