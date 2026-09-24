package com.tup.reconac.feature.usuario.mappers;

import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;
import com.tup.reconac.feature.usuario.services.domain.AuthenticatedUser;

public class UsuarioMapper {

    public static AuthResponseDto toAuthResponse(
            AuthenticatedUser user,
            String accessToken,
            String refreshToken,
            String csrfToken,
            String tokenType,
            long expirationMs) {

        return new AuthResponseDto(
                user.getId(),
                accessToken,
                refreshToken,
                tokenType,
                expirationMs,
                user.getEmail(),
                csrfToken
        );
    }

    public static RefreshResponseDto toRefreshResponse(
            String newAccessToken,
            String csrfToken,
            String tokenType,
            long expirationMs
    ) {
        return new RefreshResponseDto(
                newAccessToken,
                tokenType,
                expirationMs,
                csrfToken
        );
    }

}
