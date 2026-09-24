package com.tup.reconac.config.server.rateLimit;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.ratelimit")
@Validated
public record RateLimitProperties(
        boolean enabled,

        @Min(1)
        int capacity,

        @Min(1)
        int refillPerMinute
) {}
