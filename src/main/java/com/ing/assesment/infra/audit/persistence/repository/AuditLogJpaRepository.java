package com.ing.assesment.infra.audit.persistence.repository;

import com.ing.assesment.infra.audit.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {
}
