package com.ing.assesment.domain.audit.port;

import com.ing.assesment.domain.audit.model.AuditLog;

public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog auditLog);
}
