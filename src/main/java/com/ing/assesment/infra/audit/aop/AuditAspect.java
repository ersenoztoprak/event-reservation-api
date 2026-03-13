package com.ing.assesment.infra.audit.aop;

import com.ing.assesment.domain.audit.model.AuditLog;
import com.ing.assesment.domain.audit.port.AuditLogRepositoryPort;
import com.ing.assesment.infra.auth.security.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

@Aspect
public class AuditAspect {

    private final AuditLogRepositoryPort auditLogRepository;
    private final HttpServletRequest request;

    public AuditAspect(AuditLogRepositoryPort auditLogRepository, HttpServletRequest request) {
        this.auditLogRepository = auditLogRepository;
        this.request = request;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        Long actorId = extractActorId();
        String ip = extractClientIp();
        String userAgent = request.getHeader("User-Agent");
        String resourceId = extractResourceId(joinPoint, result);

        AuditLog auditLog = AuditLog.create(
                actorId,
                auditable.action(),
                auditable.resourceType(),
                resourceId,
                ip,
                userAgent != null ? userAgent : "unknown"
        );

        auditLogRepository.save(auditLog);

        return result;
    }

    private Long extractActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.id();
        }

        return null;
    }

    private String extractClientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private String extractResourceId(ProceedingJoinPoint joinPoint, Object result) {
        String fromResponse = extractIdFromResponse(result);
        if (fromResponse != null) {
            return fromResponse;
        }

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long l) {
                return String.valueOf(l);
            }
        }

        return "unknown";
    }

    private String extractIdFromResponse(Object result) {
        Object body = result;

        if (result instanceof ResponseEntity<?> responseEntity) {
            body = responseEntity.getBody();
        }

        if (body == null) {
            return null;
        }

        try {
            Method idMethod = body.getClass().getMethod("id");
            Object id = idMethod.invoke(body);
            return id != null ? String.valueOf(id) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
