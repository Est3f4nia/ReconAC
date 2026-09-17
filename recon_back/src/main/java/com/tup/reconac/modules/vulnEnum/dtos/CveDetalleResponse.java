package com.tup.reconac.modules.vulnEnum.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CveDetalleResponse(
        String cveId,

        String descripcion,
        String severidad,

        BigDecimal cvssScore,
        String cvssVector,

        @Schema(description = "Probabilidad EPSS entre 0 y 1; null cuando no hay dato.")
        BigDecimal epssScore,
        @Schema(description = "Indica pertenencia al catálogo CISA KEV.")
        boolean explotacionActiva,

        String versionesVulnerables,
        String mitigacion,

        String versionParche,
        String tipoParche,

        List<String> exploitRefs,
        String nistUrl,

        LocalDateTime fechaPublicacion,
        LocalDateTime ultimaModificacion
) {}
