package com.ing.assesment.infra.security.ratelimit;

import com.ing.assesment.infra.auth.security.model.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitEntry> store = new ConcurrentHashMap<>();

    public void clear() {
        store.clear();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        RateLimitPolicy policy = RateLimitPolicy.resolve(request.getMethod(), request.getRequestURI())
                .orElse(null);

        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = buildClientKey(request, policy);
        String fullKey = policy.name() + "::" + clientKey;

        Instant now = Instant.now();

        RateLimitEntry entry = store.compute(fullKey, (key, existing) -> {
            if (existing == null) {
                RateLimitEntry created = new RateLimitEntry(now);
                created.getCounter().incrementAndGet();
                return created;
            }

            Instant windowEnd = existing.getWindowStart().plus(policy.window());
            if (now.isAfter(windowEnd)) {
                existing.reset(now);
            }

            existing.getCounter().incrementAndGet();
            return existing;
        });

        if (entry.getCounter().get() > policy.maxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"title":"Too Many Requests","status":429,"detail":"Rate limit exceeded"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String buildClientKey(HttpServletRequest request, RateLimitPolicy policy) {
        if (policy.scope() == RateLimitScope.USER) {
            Long actorId = extractActorId();
            if (actorId != null) {
                return "USER:" + actorId;
            }
        }

        return "IP:" + extractClientIp(request);
    }

    private Long extractActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.id();
        }

        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
