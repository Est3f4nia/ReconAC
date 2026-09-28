package com.tup.reconac.feature.usuario.services.domain;

import com.tup.reconac.exceptions.jwt.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    public AuthenticatedUser getAuthenticatedUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {

            throw new UnauthorizedException("Usuario no autenticado");
        }

        return user;
    }

    public UUID getUsuarioId() {
        return getAuthenticatedUser().getId();
    }

}
