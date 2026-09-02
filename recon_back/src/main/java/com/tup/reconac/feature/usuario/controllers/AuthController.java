package com.tup.reconac.feature.usuario.controllers;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Value("${app.cookies.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.ok(null, "Usuario registrado correctamente"));
    }

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
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
