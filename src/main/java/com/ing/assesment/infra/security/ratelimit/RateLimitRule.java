package com.ing.assesment.infra.security.ratelimit;

import java.time.Duration;

public record RateLimitRule(
        int maxRequests,
        Duration window
) {
}
