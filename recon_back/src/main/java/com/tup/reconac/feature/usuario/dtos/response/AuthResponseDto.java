package com.tup.reconac.feature.usuario.dtos.response;

import java.util.UUID;

public record AuthResponseDto(
        UUID id,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        String email
) {}
