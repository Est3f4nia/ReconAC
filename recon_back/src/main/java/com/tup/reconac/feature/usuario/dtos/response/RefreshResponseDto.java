package com.tup.reconac.feature.usuario.dtos.response;

public record RefreshResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        String csrfToken
) {}
