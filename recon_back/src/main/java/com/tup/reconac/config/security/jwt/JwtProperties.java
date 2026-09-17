package com.tup.reconac.config.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs,
        String algorithm
) {}
