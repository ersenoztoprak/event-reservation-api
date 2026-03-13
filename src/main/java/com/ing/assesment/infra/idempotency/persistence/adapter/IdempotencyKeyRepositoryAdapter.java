package com.ing.assesment.infra.idempotency.persistence.adapter;

import com.ing.assesment.domain.idempotency.model.IdempotencyKeyRecord;
import com.ing.assesment.domain.idempotency.port.IdempotencyKeyRepositoryPort;
import com.ing.assesment.infra.idempotency.persistence.entity.IdempotencyKeyEntity;
import com.ing.assesment.infra.idempotency.persistence.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@RequiredArgsConstructor
public class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepositoryPort {

    private final IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Override
    public Optional<IdempotencyKeyRecord> findByKeyAndEndpoint(String key, String endpoint) {
        return idempotencyKeyJpaRepository.findByKeyAndEndpoint(key, endpoint)
                .map(this::toDomain);
    }

    @Override
    public IdempotencyKeyRecord save(IdempotencyKeyRecord record) {
        try {
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
            entity.setId(record.getId());
            entity.setKey(record.getKey());
            entity.setEndpoint(record.getEndpoint());
            entity.setRequestHash(record.getRequestHash());
            entity.setResponseHash(record.getResponseHash());
            entity.setStatus(record.getStatus());
            entity.setCreatedAt(record.getCreatedAt());
            entity.setTtl(record.getTtl());

            IdempotencyKeyEntity saved = idempotencyKeyJpaRepository.saveAndFlush(entity);
            return toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new com.ing.assesment.domain.common.exception.IdempotencyConflictException(
                    "Duplicate idempotency key for this endpoint"
            );
        }
    }

    private IdempotencyKeyRecord toDomain(IdempotencyKeyEntity entity) {
        IdempotencyKeyRecord record = IdempotencyKeyRecord.start(
                entity.getKey(),
                entity.getEndpoint(),
                entity.getRequestHash(),
                entity.getTtl()
        );
        record.setId(entity.getId());
        record.setCreatedAt(entity.getCreatedAt());
        record.setResponseHash(entity.getResponseHash());
        record.setStatus(entity.getStatus());
        return record;
    }
}
