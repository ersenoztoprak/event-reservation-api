package com.ing.assesment.domain.idempotency.port;

import com.ing.assesment.domain.idempotency.model.IdempotencyKeyRecord;

import java.util.Optional;

public interface IdempotencyKeyRepositoryPort {

    Optional<IdempotencyKeyRecord> findByKeyAndEndpoint(String key, String endpoint);

    IdempotencyKeyRecord save(IdempotencyKeyRecord record);
}
