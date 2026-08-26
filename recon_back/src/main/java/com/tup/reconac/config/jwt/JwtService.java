package com.tup.reconac.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initSigningKey() {
        if (!"HS256".equalsIgnoreCase(properties.algorithm())) {
            throw new IllegalStateException("Solo está soportado el algoritmo HS256 (app.jwt.algorithm)");
        }
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes en UTF-8 para HS256");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.expirationMs());
        return Jwts.builder()
                .subject(username)
                .claim(JwtClaimNames.ROLES, List.copyOf(roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.refreshExpirationMs());
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Optional<Claims> parseValidClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                    Jwts.parser()
                            .verifyWith(signingKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload());
        } catch (JwtException ignored) {
            return Optional.empty();
        }
    }

    public Optional<String> extractEmail(String token) {
        return parseValidClaims(token).map(Claims::getSubject);
    }

    public List<String> extractRoles(Claims claims) {
        Object raw = claims.get(JwtClaimNames.ROLES);
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
