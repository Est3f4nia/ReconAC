package com.tup.reconac.feature.auth;

import tools.jackson.databind.ObjectMapper;
import com.tup.reconac.feature.usuario.dtos.request.LoginRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RefreshRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RegisterRequestDto;
import com.tup.reconac.feature.usuario.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController - Tests de integración (H2 embebido)")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    // ========================
    // REGISTER
    // ========================

    @Test
    @DisplayName("POST /api/auth/register - Happy path: registro exitoso retorna 201")
    void register_whenValidRequest_returns201() throws Exception {
        RegisterRequestDto request =
                new RegisterRequestDto("test@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Usuario registrado correctamente"));

        assertTrue(
                usuarioRepository.findByEmail("test@test.com").isPresent()
        );
    }

    @Test
    @DisplayName("POST /api/auth/register - Error: email duplicado retorna 409")
    void register_whenEmailAlreadyExists_returns409() throws Exception {
        RegisterRequestDto request =
                new RegisterRequestDto("dup@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertEquals(
                1,
                usuarioRepository.findAll().size()
        );
    }

    @Test
    @DisplayName("POST /api/auth/register - Error: email inválido retorna 400")
    void register_whenInvalidEmail_returns400() throws Exception {
        RegisterRequestDto request =
                new RegisterRequestDto("not-an-email", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertTrue(usuarioRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("POST /api/auth/register - Error: campos nulos retorna 400")
    void register_whenNullFields_returns400() throws Exception {
        String request = "{\"email\":null,\"contrasenia\":null}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertTrue(usuarioRepository.findAll().isEmpty());
    }

    // ========================
    // LOGIN
    // ========================

    @Test
    @DisplayName("POST /api/auth/login - Happy path: login exitoso retorna 200 y cookies HttpOnly")
    void login_whenValidCredentials_returns200AndCookies() throws Exception {
        RegisterRequestDto registerReq =
                new RegisterRequestDto("login@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequestDto loginReq =
                new LoginRequestDto("login@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Autenticación correcta"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    @DisplayName("POST /api/auth/login - Error: credenciales inválidas retorna 401")
    void login_whenBadCredentials_returns401() throws Exception {
        RegisterRequestDto registerReq =
                new RegisterRequestDto("login2@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequestDto loginReq =
                new LoginRequestDto("login2@test.com", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("access_token"))
                .andExpect(cookie().doesNotExist("refresh_token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Error: campos vacíos retorna 400")
    void login_whenEmptyFields_returns400() throws Exception {
        LoginRequestDto loginReq =
                new LoginRequestDto("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest());
    }

    // ========================
    // REFRESH
    // ========================

    @Test
    @DisplayName("POST /api/auth/refresh - Happy path: renueva token retorna 200")
    void refresh_whenValidToken_returns200() throws Exception {
        RegisterRequestDto registerReq =
                new RegisterRequestDto("refresh@test.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequestDto loginReq =
                new LoginRequestDto("refresh@test.com", "password123");

        String loginResponse =
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String refreshToken =
                objectMapper.readTree(loginResponse)
                        .path("data")
                        .path("refreshToken")
                        .asString();

        assertFalse(refreshToken.isBlank());

        RefreshRequestDto refreshReq =
                new RefreshRequestDto(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Token renovado correctamente"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Error: token inválido retorna 401")
    void refresh_whenInvalidToken_returns401() throws Exception {
        RefreshRequestDto refreshReq =
                new RefreshRequestDto("invalid-token-value");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Error: token vacío retorna 400")
    void refresh_whenEmptyToken_returns400() throws Exception {
        RefreshRequestDto refreshReq =
                new RefreshRequestDto("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isBadRequest());
    }
}