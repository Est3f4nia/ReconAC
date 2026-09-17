package com.tup.reconac.feature.usuario.services;

import com.tup.reconac.config.security.jwt.JwtProperties;
import com.tup.reconac.config.security.jwt.JwtService;
import com.tup.reconac.exceptions.jwt.InvalidCredentialsException;
import com.tup.reconac.exceptions.usuario.UserAlreadyExistsException;
import com.tup.reconac.feature.usuario.dtos.request.*;
import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;
import com.tup.reconac.feature.usuario.models.Usuario;
import java.util.UUID;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.interfaces.IAuthService;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService implements IAuthService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final NvdApiKeyEncryptionService encryptionService;

    @Transactional
    @Override
    public void register(RegisterRequestDto request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("El usuario ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(request.email());
        usuario.setContrasenia(passwordEncoder.encode(request.contrasenia()));
        usuario.setNvdApiKey(encryptionService.encrypt(request.apiKey()));
        usuarioRepository.save(usuario);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.contrasenia())
            );
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            assert principal != null;
            var roles = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            String accessToken = jwtService.generateToken(principal.getUsername(), roles);
            String refreshToken = jwtService.generateRefreshToken(principal.getUsername());
            String csrfToken = UUID.randomUUID().toString();
            Usuario usuario = (Usuario) principal;
            return new AuthResponseDto(
                    usuario.getId(),
                    accessToken,
                    refreshToken,
                    TOKEN_TYPE_BEARER,
                    jwtProperties.expirationMs(),
                    usuario.getEmail(),
                    csrfToken
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
    }

    @Override
    public RefreshResponseDto refresh(RefreshRequestDto request) {
        Claims claims = jwtService.parseValidClaims(request.refreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Token inválido o expirado"));
        String username = claims.getSubject();
        usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario no encontrado"));
        var roles = jwtService.extractRoles(claims);
        String newAccessToken = jwtService.generateToken(username, roles);
        String csrfToken = UUID.randomUUID().toString();
        return new RefreshResponseDto(
                newAccessToken,
                "Bearer",
                jwtProperties.expirationMs(),
                csrfToken
        );
    }
}
