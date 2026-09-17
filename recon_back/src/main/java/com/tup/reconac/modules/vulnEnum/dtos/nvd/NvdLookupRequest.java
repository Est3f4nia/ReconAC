package com.tup.reconac.modules.vulnEnum.dtos.nvd;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NvdLookupRequest(
        @Schema(description = "UUID persistido del escaneo. Permite resolver la clave NVD de su propietario; no enviar jobId.")
        UUID escaneoId,
        List<String> cpes,
        @Schema(description = "Años de antigüedad máxima; 0 desactiva el filtro temporal.")
        int maxCveYears,
        @Schema(description = "Umbral CVSS inclusivo entre 0 y 10; null equivale a 0.")
        BigDecimal minCvssScore
) {
}
