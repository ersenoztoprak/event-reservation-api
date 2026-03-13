package com.ing.assesment.infra.idempotency.persistence.repository;

import com.ing.assesment.infra.idempotency.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    Optional<IdempotencyKeyEntity> findByKeyAndEndpoint(String key, String endpoint);
}
