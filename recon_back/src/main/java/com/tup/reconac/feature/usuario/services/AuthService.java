package com.tup.reconac.feature.usuario.services;

import com.tup.reconac.config.security.jwt.JwtProperties;
import com.tup.reconac.config.security.jwt.JwtService;
import com.tup.reconac.exceptions.jwt.InvalidCredentialsException;
import com.tup.reconac.exceptions.usuario.KeyNotValidException;
import com.tup.reconac.exceptions.usuario.UserAlreadyExistsException;
import com.tup.reconac.feature.usuario.dtos.request.*;
import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;
import com.tup.reconac.feature.usuario.mappers.UsuarioMapper;
import com.tup.reconac.feature.usuario.models.Usuario;

import java.util.List;
import java.util.UUID;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.domain.AuthenticatedUser;
import com.tup.reconac.feature.usuario.services.domain.CustomUserDetailsService;
import com.tup.reconac.feature.usuario.services.domain.UsuarioKeyService;
import com.tup.reconac.feature.usuario.services.interfaces.IAuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioKeyService usuarioKeyService;
    private final CustomUserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    @Override
    public void register(RegisterRequestDto request) {

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El usuario ya existe");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setContrasenia(passwordEncoder.encode(request.contrasenia()));

        usuarioKeyService.updateKey(usuario, request.apiKey());
        usuarioRepository.save(usuario);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.email(),
                            request.contrasenia()
                    )
            );

            if (!(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
                throw new IllegalStateException("Principal de autenticación inválido");
            }

            List<String> roles = extractRoles(principal);

            String accessToken = jwtService.generateToken(principal.getUsername(), roles);
            String refreshToken = jwtService.generateRefreshToken(principal.getUsername());
            String csrfToken = UUID.randomUUID().toString();

            return UsuarioMapper.toAuthResponse(
                    principal,
                    accessToken,
                    refreshToken,
                    csrfToken,
                    TOKEN_TYPE_BEARER,
                    jwtProperties.expirationMs()
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
    }

    @Override
    public RefreshResponseDto refresh(RefreshRequestDto request) {

        Claims claims = jwtService.parseValidClaims(request.refreshToken())
                .orElseThrow(() ->
                        new KeyNotValidException("Token inválido o expirado"));

        String email = claims.getSubject();
        AuthenticatedUser user = userDetailsService.loadUserByUsername(email);

        String newAccessToken = jwtService.generateToken(
                user.getUsername(),
                extractRoles(user)
        );

        String csrfToken = UUID.randomUUID().toString();

        return UsuarioMapper.toRefreshResponse(
                newAccessToken,
                csrfToken,
                TOKEN_TYPE_BEARER,
                jwtProperties.expirationMs()
        );
    }

    private List<String> extractRoles(UserDetails user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}