package com.tup.reconac.modules.vulnEnum.dtos.metricas;

import java.math.BigDecimal;
import java.util.List;

public record HostVulnerabilidadResponse(
        String ip,
        String hostname,
        long vulnerabilidades,
        long vulnerabilidadesCriticas,
        BigDecimal cvssMaximo,
        BigDecimal epssMaximo,
        boolean kev,
        List<HostCveResponse> cvesCriticas,
        List<HostCveResponse> cvesPrioritarias
) {
}