package com.tup.reconac.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF con patrón Double Submit Cookie.
 * El backend emite en /login/refresh una cookie `XSRF-TOKEN` (no httpOnly, SameSite=Strict)
 * con un token aleatorio. El cliente (SPA) debe leerla y reenviarla en el header
 * `X-XSRF-TOKEN` en cada request mutante. El filtro valida que header y cookie coincidan.
 * Solo aplica a sesiones basadas en cookie (`access_token`). Los clientes que usan
 * Bearer (no-browser) no son susceptibles a CSRF y se dejan pasar.
 */

@Component
public class CsrfFilter extends OncePerRequestFilter {

    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String ACCESS_COOKIE = "access_token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,    // deprecated, ver
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = findCookie(request, ACCESS_COOKIE);
        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerToken = request.getHeader(CSRF_HEADER);
        String cookieToken = findCookie(request, CSRF_COOKIE);

        if (headerToken == null || !headerToken.equals(cookieToken)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"type\":\"/errors/csrf\",\"title\":\"Forbidden\"," +
                    "\"status\":403,\"detail\":\"Token CSRF inválido o ausente.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String findCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
