package com.ing.assesment.domain.idempotency.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class IdempotencyKeyRecord {

    private Long id;
    private String key;
    private String endpoint;
    private String requestHash;
    private String responseHash;
    private IdempotencyStatus status;
    private Instant createdAt;
    private Instant ttl;

    private IdempotencyKeyRecord() {
    }

    private IdempotencyKeyRecord(String key, String endpoint, String requestHash, Instant ttl) {
        this.key = key;
        this.endpoint = endpoint;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
        this.ttl = ttl;
    }

    public static IdempotencyKeyRecord start(String key, String endpoint, String requestHash, Instant ttl) {
        return new IdempotencyKeyRecord(key, endpoint, requestHash, ttl);
    }

    public boolean isSameRequest(String requestHash) {
        return this.requestHash != null && this.requestHash.equals(requestHash);
    }

    public boolean isExpired(Instant now) {
        return ttl != null && ttl.isBefore(now);
    }

    public void markCompleted(String responseHash) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseHash = responseHash;
    }

    public void markFailed() {
        this.status = IdempotencyStatus.FAILED;
    }

}
