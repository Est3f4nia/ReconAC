package com.tup.reconac.modules.vulnEnum.dtos;

import java.util.List;

public record HostDesgloseResponse(
        List<HostVulnerabilidadResponse> masVulnerabilidadesCriticas,
        List<HostVulnerabilidadResponse> mayorRiesgoExplotacion
) {
}