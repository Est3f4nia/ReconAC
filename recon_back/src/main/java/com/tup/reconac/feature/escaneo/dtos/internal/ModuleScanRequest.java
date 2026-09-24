package com.tup.reconac.feature.escaneo.dtos.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ModuleScanRequest(
        @JsonProperty("job_id")
        String jobId,

        @JsonProperty("escaneo_id")
        String escaneoId,

        @JsonProperty("targets")
        List<String> targets,

        @JsonProperty("timeout")
        Integer timeout,

        @JsonProperty("icmp_timeout")
        Integer icmpTimeout,

        @JsonProperty("max_cve_years")
        Integer maxCveYears,

        @JsonProperty("min_cvss_score")
        Double minCvssScore
) {}
