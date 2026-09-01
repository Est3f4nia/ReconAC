package com.tup.reconac.feature.auth;

import com.tup.reconac.config.jwt.JwtProperties;
import com.tup.reconac.config.jwt.JwtService;
import com.tup.reconac.exceptions.jwt.InvalidCredentialsException;
import com.tup.reconac.exceptions.usuario.UserAlreadyExistsException;
import com.tup.reconac.feature.usuario.dtos.request.LoginRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RefreshRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RegisterRequestDto;
import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;
import com.tup.reconac.feature.usuario.models.Usuario;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import com.tup.reconac.feature.usuario.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests unitarios")
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;
    private RegisterRequestDto registerRequest;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("test@test.com");
        usuario.setContrasenia("$2a$10$encodedPassword");

        registerRequest = new RegisterRequestDto(
                "test@test.com",
                "password123"
        );

        loginRequest = new LoginRequestDto(
                "test@test.com",
                "password123"
        );
    }

    // ========================
    // REGISTER
    // ========================

    @Test
    @DisplayName("Register - Happy path: registra usuario correctamente")
    void register_whenValidData_savesUser() {
        when(usuarioRepository.existsByEmail("test@test.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("$2a$10$encodedPassword");

        authService.register(registerRequest);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario saved = captor.getValue();

        assertEquals(registerRequest.email(), saved.getEmail());
        assertEquals(
                "$2a$10$encodedPassword",
                saved.getContrasenia()
        );

        verify(usuarioRepository).existsByEmail("test@test.com");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Register - Error: email ya existe")
    void register_whenEmailExists_throwsUserAlreadyExistsException() {
        when(usuarioRepository.existsByEmail("test@test.com"))
                .thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals(
                "El usuario ya existe",
                exception.getMessage()
        );

        verify(usuarioRepository).existsByEmail("test@test.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    // ========================
    // LOGIN
    // ========================

    @Test
    @DisplayName("Login - Happy path: login exitoso devuelve tokens")
    void login_whenValidCredentials_returnsTokens() {
        Authentication mockAuth = mock(Authentication.class);

        doReturn(usuario)
                .when(mockAuth)
                .getPrincipal();

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        when(jwtService.generateToken(anyString(), anyList()))
                .thenReturn("access-token");

        when(jwtService.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        when(jwtProperties.expirationMs())
                .thenReturn(86400000L);

        AuthResponseDto response =
                authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("test@test.com", response.email());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        verify(jwtService).generateToken(
                anyString(),
                anyList()
        );

        verify(jwtService).generateRefreshToken(
                "test@test.com"
        );
    }

    @Test
    @DisplayName("Login - Error: credenciales inválidas")
    void login_whenBadCredentials_throwsInvalidCredentialsException() {
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(
                        new BadCredentialsException("Bad credentials")
                );

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(
                "Credenciales inválidas",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(anyString(), anyList());

        verify(jwtService, never())
                .generateRefreshToken(anyString());
    }

    // ========================
    // REFRESH
    // ========================

    @Test
    @DisplayName("Refresh - Happy path: renueva access token")
    void refresh_whenValidRefreshToken_returnsNewAccessToken() {
        RefreshRequestDto refreshRequest =
                new RefreshRequestDto("valid-refresh-token");

        io.jsonwebtoken.Claims mockClaims =
                mock(io.jsonwebtoken.Claims.class);

        doReturn("test@test.com")
                .when(mockClaims)
                .getSubject();

        when(jwtService.parseValidClaims("valid-refresh-token"))
                .thenReturn(Optional.of(mockClaims));

        when(usuarioRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(usuario));

        when(jwtService.generateToken(anyString(), anyList()))
                .thenReturn("new-access-token");

        when(jwtProperties.expirationMs())
                .thenReturn(86400000L);

        RefreshResponseDto response =
                authService.refresh(refreshRequest);

        assertNotNull(response);
        assertEquals(
                "new-access-token",
                response.accessToken()
        );
        assertEquals(
                "Bearer",
                response.tokenType()
        );

        verify(jwtService)
                .parseValidClaims("valid-refresh-token");

        verify(usuarioRepository)
                .findByEmail("test@test.com");

        verify(jwtService)
                .generateToken(anyString(), anyList());
    }

    @Test
    @DisplayName("Refresh - Error: token inválido")
    void refresh_whenInvalidToken_throwsInvalidCredentialsException() {
        RefreshRequestDto refreshRequest =
                new RefreshRequestDto("invalid-token");

        when(jwtService.parseValidClaims("invalid-token"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.refresh(refreshRequest)
        );

        assertEquals(
                "Token inválido o expirado",
                exception.getMessage()
        );

        verify(jwtService)
                .parseValidClaims("invalid-token");

        verify(usuarioRepository, never())
                .findByEmail(anyString());

        verify(jwtService, never())
                .generateToken(anyString(), anyList());
    }
}