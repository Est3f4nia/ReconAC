package com.tup.reconac.feature.usuario.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.usuario.dtos.request.LoginRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RefreshRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RegisterRequestDto;
import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;
import com.tup.reconac.feature.usuario.services.interfaces.IAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth / Usuario")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Value("${app.cookies.secure:true}")
    private boolean cookieSecure;

    @Operation(summary = "Registrar un usuario", description = "Público. Requiere email, contrasenia y apiKey NVD según las validaciones actuales. La clave se almacena cifrada.")
    @ApiResponse(responseCode = "201", description = "Creado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Error409")
    @ApiResponse(responseCode = "429", ref = "#/components/responses/Error429")
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok(null, "Usuario registrado correctamente"));
    }

    @Operation(summary = "Iniciar sesión", description = "Público. Devuelve tokens y establece cookies access_token, refresh_token y XSRF-TOKEN. Para escrituras con cookie access_token, enviar X-XSRF-TOKEN con el valor de XSRF-TOKEN.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401")
    @ApiResponse(responseCode = "429", ref = "#/components/responses/Error429")
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        AuthResponseDto body = authService.login(request);
        addCookie(response, "access_token", body.accessToken(), 86400);
        addCookie(response, "refresh_token", body.refreshToken(), 604800);
        addCsrfCookie(response, body.csrfToken());
        return ResponseEntity.ok(BaseResponse.ok(body, "Autenticación correcta"));
    }

    @Operation(summary = "Renovar el access token", description = "Público. Requiere refreshToken en el cuerpo, incluso si existe una cookie refresh_token. Renueva access_token y XSRF-TOKEN.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Error401")
    @ApiResponse(responseCode = "429", ref = "#/components/responses/Error429")
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<RefreshResponseDto>> refresh(
            @Valid @RequestBody RefreshRequestDto request,
            HttpServletResponse response) {
        RefreshResponseDto body = authService.refresh(request);
        addCookie(response, "access_token", body.accessToken(), 86400);
        addCsrfCookie(response, body.csrfToken());
        return ResponseEntity.ok(BaseResponse.ok(body, "Token renovado correctamente"));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void addCsrfCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie("XSRF-TOKEN", value);
        cookie.setHttpOnly(false);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
