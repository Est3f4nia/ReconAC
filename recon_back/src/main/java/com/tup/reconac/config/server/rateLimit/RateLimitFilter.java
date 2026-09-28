package com.tup.reconac.config.server.rateLimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting por IP sobre los endpoints públicos de autenticación, para mitigar
 * brute force. Usa Bucket4J en memoria (por instancia). Para un entorno
 * distribuido (varias réplicas) convendría respaldar los buckets en Redis.
 * Se registra como servlet filter solo para "/api/auth/register-login" vía FilterRegistrationBean.
 */

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public RateLimitFilter(
            RateLimitProperties props,
            ObjectMapper objectMapper) {

        this.props = props;
        this.objectMapper = objectMapper;
    }

    private Bucket newBucket() {

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(props.capacity())
                .refillGreedy(props.refillPerMinute(), Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!props.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.get(ip, key -> newBucket());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(
                    "X-RateLimit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );

            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1,
                TimeUnit.NANOSECONDS.toSeconds(
                        probe.getNanosToWaitForRefill()
                )
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(retryAfterSeconds)
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos. Intente nuevamente más tarde."
        );

        problem.setTitle("Too Many Requests");
        problem.setType(URI.create("/errors/rate-limit"));

        objectMapper.writeValue(response.getWriter(), problem);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod());
    }
}
