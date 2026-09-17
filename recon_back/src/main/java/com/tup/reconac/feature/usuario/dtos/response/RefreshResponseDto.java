package com.tup.reconac.feature.usuario.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshResponseDto(
        String accessToken,
        String tokenType,
        @Schema(description = "Vigencia del access token en MILISEGUNDOS, conservando el nombre del contrato existente.")
        long expiresIn,
        String csrfToken
) {}
