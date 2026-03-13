package com.ing.assesment.infra.idempotency.persistence.entity;

import com.ing.assesment.domain.idempotency.model.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key_endpoint", columnNames = {"idempotencyKey", "endpoint"})
        }
)
@Getter
@Setter
public class IdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotencyKey", nullable = false)
    private String key;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String requestHash;

    @Column
    private String responseHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant ttl;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
