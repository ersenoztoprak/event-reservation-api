package com.ing.assesment.infra.security.ratelimit;

import lombok.Setter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitEntry {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Setter
    private Instant windowStart;

    public RateLimitEntry(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public AtomicInteger getCounter() {
        return counter;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void reset(Instant newWindowStart) {
        counter.set(0);
        this.windowStart = newWindowStart;
    }
}
