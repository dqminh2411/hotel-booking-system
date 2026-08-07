package com.hotelbooking.chassis.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration cho module Audit.
 *
 * Tự động đăng ký các bean: AuditLogger, SpanEventPublisher, ActorResolver,
 * RequestResolver, AuditLogAspect khi có AOP trên classpath.
 * 
 *
 * 
 * Có thể tắt toàn bộ bằng property: chassis.audit.enabled=false
 * 
 */
@AutoConfiguration
@ConditionalOnProperty(name = "chassis.audit.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
public class AuditAutoConfiguration {

    @Bean
    public AuditLogger auditLogger() {
        return new AuditLogger();
    }

    @Bean
    public SpanEventPublisher spanEventPublisher() {
        return new SpanEventPublisher();
    }

    @Bean
    public ActorResolver actorResolver() {
        return new ActorResolver();
    }

    @Bean
    public RequestResolver requestResolver() {
        return new RequestResolver();
    }

    @Bean
    public AuditLogAspect auditLogAspect(AuditLogger auditLogger,
            SpanEventPublisher spanEventPublisher,
            ActorResolver actorResolver,
            RequestResolver requestResolver) {
        return new AuditLogAspect(auditLogger, spanEventPublisher, actorResolver, requestResolver);
    }
}
