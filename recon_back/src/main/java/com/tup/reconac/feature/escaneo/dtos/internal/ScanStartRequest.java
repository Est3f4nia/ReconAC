package com.tup.reconac.feature.escaneo.dtos.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScanStartRequest(
        @NotEmpty(message = "objetivos no puede estar vacío")
        List<String> objetivos,

        @Schema(description = "Tiempo máximo en segundos de cada fase Nmap por objetivo; por defecto 600.")
        Integer timeout,

        @Schema(description = "Espera ICMP en segundos; por defecto 5.")
        Integer icmpTimeout,

        @Schema(description = "Antigüedad máxima de CVEs en años; por defecto 2. Cero desactiva el filtro temporal.")
        Integer maxCveYears,

        @Schema(description = "Umbral CVSS inclusivo de 0 a 10; por defecto 0.")
        Double minCvssScore,

        @Schema(description = "Campo conservado por compatibilidad. El servicio actual obtiene la clave del usuario persistido y no utiliza este campo.")
        String nvdApiKey
) {}
