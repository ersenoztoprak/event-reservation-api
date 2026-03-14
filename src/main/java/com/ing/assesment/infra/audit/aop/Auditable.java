package com.ing.assesment.infra.audit.aop;


import com.ing.assesment.domain.audit.model.AuditAction;
import com.ing.assesment.domain.audit.model.AuditResourceType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    AuditAction action();

    AuditResourceType resourceType();
}
