package com.ing.assesment.infra.audit.persistence.adapter;

import com.ing.assesment.domain.audit.model.AuditLog;
import com.ing.assesment.domain.audit.port.AuditLogRepositoryPort;
import com.ing.assesment.infra.audit.persistence.entity.AuditLogEntity;
import com.ing.assesment.infra.audit.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final AuditLogJpaRepository auditLogJpaRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(auditLog.getId());
        entity.setActorId(auditLog.getActorId());
        entity.setAction(auditLog.getAction());
        entity.setResourceType(auditLog.getResourceType());
        entity.setResourceId(auditLog.getResourceId());
        entity.setIp(auditLog.getIp());
        entity.setUserAgent(auditLog.getUserAgent());
        entity.setCreatedAt(auditLog.getCreatedAt());

        AuditLogEntity saved = auditLogJpaRepository.save(entity);

        AuditLog result = AuditLog.create(
                saved.getActorId(),
                saved.getAction(),
                saved.getResourceType(),
                saved.getResourceId(),
                saved.getIp(),
                saved.getUserAgent()
        );
        result.setId(saved.getId());
        return result;
    }
}
