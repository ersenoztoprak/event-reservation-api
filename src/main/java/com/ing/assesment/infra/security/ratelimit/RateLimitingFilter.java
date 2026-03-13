package com.ing.assesment.infra.security.ratelimit;

import com.ing.assesment.infra.auth.security.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
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

    private final Map<String, RateLimitRule> rules = Map.of(
            "POST:/api/auth/login", new RateLimitRule(10, java.time.Duration.ofMinutes(1)),
            "POST:/api/auth/register", new RateLimitRule(10, java.time.Duration.ofMinutes(1)),
            "POST:/api/auth/refresh", new RateLimitRule(10, java.time.Duration.ofMinutes(1)),
            "POST:/api/events/*/reservations", new RateLimitRule(10, java.time.Duration.ofMinutes(1))
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ruleKey = resolveRuleKey(request);
        if (ruleKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitRule rule = rules.get(ruleKey);
        String clientKey = buildClientKey(request, ruleKey);
        String fullKey = ruleKey + "::" + clientKey;

        Instant now = Instant.now();

        RateLimitEntry entry = store.compute(fullKey, (key, existing) -> {
            if (existing == null) {
                RateLimitEntry created = new RateLimitEntry(now);
                created.getCounter().incrementAndGet();
                return created;
            }

            Instant windowEnd = existing.getWindowStart().plus(rule.window());
            if (now.isAfter(windowEnd)) {
                existing.reset(now);
            }

            existing.getCounter().incrementAndGet();
            return existing;
        });

        if (entry.getCounter().get() > rule.maxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"title":"Too Many Requests","status":429,"detail":"Rate limit exceeded"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    public void clear() {
        store.clear();
    }

    private String resolveRuleKey(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        String exact = method + ":" + uri;
        if (rules.containsKey(exact)) {
            return exact;
        }

        if (HttpMethod.POST.matches(method) && uri.matches("^/api/events/\\d+/reservations$")) {
            return "POST:/api/events/*/reservations";
        }

        return null;
    }

    private String buildClientKey(HttpServletRequest request, String ruleKey) {
        if ("POST:/api/events/*/reservations".equals(ruleKey)) {
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
