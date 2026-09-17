package com.tup.reconac.config.server.rateLimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(
        boolean enabled,
        int capacity,
        int refillPerMinute
) {}
