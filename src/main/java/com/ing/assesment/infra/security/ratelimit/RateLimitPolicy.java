package com.ing.assesment.infra.security.ratelimit;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public enum RateLimitPolicy {

    LOGIN("POST", "/api/auth/login", 10, Duration.ofMinutes(1), RateLimitScope.IP),
    REGISTER("POST", "/api/auth/register", 10, Duration.ofMinutes(1), RateLimitScope.IP),
    REFRESH("POST", "/api/auth/refresh", 10, Duration.ofMinutes(1), RateLimitScope.IP),
    CREATE_RESERVATION("POST", "^/api/events/\\d+/reservations$", 10, Duration.ofMinutes(1), RateLimitScope.USER);

    private final String method;
    private final String pathPattern;
    private final int maxRequests;
    private final Duration window;
    private final RateLimitScope scope;
    private final Pattern compiledPattern;

    RateLimitPolicy(String method,
                    String pathPattern,
                    int maxRequests,
                    Duration window,
                    RateLimitScope scope) {
        this.method = method;
        this.pathPattern = pathPattern;
        this.maxRequests = maxRequests;
        this.window = window;
        this.scope = scope;
        this.compiledPattern = Pattern.compile(pathPattern);
    }

    public static Optional<RateLimitPolicy> resolve(String method, String uri) {
        return Arrays.stream(values())
                .filter(policy -> policy.method.equalsIgnoreCase(method))
                .filter(policy -> policy.matches(uri))
                .findFirst();
    }

    public boolean matches(String uri) {
        if (pathPattern.startsWith("^")) {
            return compiledPattern.matcher(uri).matches();
        }
        return pathPattern.equals(uri);
    }

    public int maxRequests() {
        return maxRequests;
    }

    public Duration window() {
        return window;
    }

    public RateLimitScope scope() {
        return scope;
    }

}
