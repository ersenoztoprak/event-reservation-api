package com.ing.assesment.domain.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AuditLog {

    private Long id;
    private Long actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String ip;
    private String userAgent;
    private Instant createdAt;

    private AuditLog() {
    }

    private AuditLog(Long actorId,
                     String action,
                     String resourceType,
                     String resourceId,
                     String ip,
                     String userAgent) {
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ip = ip;
        this.userAgent = userAgent;
        this.createdAt = Instant.now();
    }

    public static AuditLog create(Long actorId,
                                  String action,
                                  String resourceType,
                                  String resourceId,
                                  String ip,
                                  String userAgent) {
        return new AuditLog(actorId, action, resourceType, resourceId, ip, userAgent);
    }
}
