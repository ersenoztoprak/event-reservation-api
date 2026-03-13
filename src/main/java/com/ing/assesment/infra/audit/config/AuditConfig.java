package com.ing.assesment.infra.audit.config;

import com.ing.assesment.domain.audit.port.AuditLogRepositoryPort;
import com.ing.assesment.infra.audit.aop.AuditAspect;
import com.ing.assesment.infra.audit.persistence.adapter.AuditLogRepositoryAdapter;
import com.ing.assesment.infra.audit.persistence.repository.AuditLogJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

    @Bean
    public AuditLogRepositoryPort auditLogRepositoryPort(AuditLogJpaRepository auditLogJpaRepository) {
        return new AuditLogRepositoryAdapter(auditLogJpaRepository);
    }

    @Bean
    public AuditAspect auditAspect(AuditLogRepositoryPort auditLogRepositoryPort,
                                   HttpServletRequest request) {
        return new AuditAspect(auditLogRepositoryPort, request);
    }
}
