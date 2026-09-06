package com.tup.reconac.modules.vulnEnum.dtos;

import java.math.BigDecimal;

public record HostVulnerabilidadResponse(
        String ip,
        String hostname,
        long vulnerabilidades,
        long vulnerabilidadesCriticas,
        BigDecimal cvssMaximo,
        BigDecimal epssMaximo
) {}
