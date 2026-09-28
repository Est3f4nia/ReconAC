package com.tup.reconac.feature.escaneo.dtos.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoverEscaneoRequest(
        @NotNull UUID auditoriaId
) {}