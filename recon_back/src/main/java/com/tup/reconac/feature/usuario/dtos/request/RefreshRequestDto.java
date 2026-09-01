package com.tup.reconac.feature.usuario.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken
) {}
