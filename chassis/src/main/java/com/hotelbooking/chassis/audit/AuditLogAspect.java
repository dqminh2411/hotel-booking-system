package com.hotelbooking.chassis.audit;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trung tâm của module audit.
 *
 * Thực hiện:
 * 
 * <ul>
 * <li><b>Before</b>: lưu startTime</li>
 * <li><b>After Returning</b>: tính duration, tạo AuditLogEntry, gọi
 * AuditLogger, gọi SpanEventPublisher</li>
 * <li><b>After Throwing</b>: tạo AuditLogEntry lỗi, AuditLogger,
 * Span.recordException(), Span.setStatus(ERROR), SpanEventPublisher</li>
 * </ul>
 *
 * 
 * Hỗ trợ SpEL expression để trích xuất targetId và extraData từ method
 * parameter/return value.
 * 
 * 
 * Business không cần gọi logger trực tiếp.
 * 
 * 
 * Exception vẫn được throw lại sau khi audit.
 * 
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogger auditLogger;
    private final SpanEventPublisher spanEventPublisher;
    private final ActorResolver actorResolver;
    private final RequestResolver requestResolver;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        String spanName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        Tracer tracer = GlobalOpenTelemetry.getTracer("hotelbooking-chassis-audit");
        Span auditSpan = tracer.spanBuilder(spanName)
                .setAttribute("audit.event_type", auditLog.eventType().name())
                .setAttribute("audit.target_type", auditLog.targetType().name())
                .startSpan();

        // Resolve actor và request info
        String actorId = actorResolver.resolveActorId();
        String actorType = actorResolver.resolveActorType();
        String requestIp = requestResolver.resolveRequestIp();

        // Tạo OTel Baggage chứa actorId, actorType, requestIp để tự động lan truyền qua Outbox / Kafka
        BaggageBuilder baggageBuilder = Baggage.current().toBuilder();
        if (actorId != null && !"anonymous".equals(actorId)) {
            baggageBuilder.put("actorId", actorId);
        }
        if (actorType != null && !"ANONYMOUS".equals(actorType)) {
            baggageBuilder.put("actorType", actorType);
        }
        if (requestIp != null && !"unknown".equals(requestIp)) {
            baggageBuilder.put("requestIp", requestIp);
        }
        Baggage baggage = baggageBuilder.build();

        try (Scope scope = auditSpan.makeCurrent(); Scope baggageScope = baggage.makeCurrent()) {
            // Thực thi business method (các DB queries, Spring Data repo, Kafka publish... nằm dưới auditSpan và baggage!)
            Object result = pjp.proceed();

            // Tính duration
            long durationMs = System.currentTimeMillis() - startTime;

            // Resolve SpEL (có result)
            String targetId = resolveTargetId(auditLog, method, pjp.getArgs(), result);
            Map<String, Object> extraData = resolveExtraData(auditLog, method, pjp.getArgs(), result);

            if (targetId != null) {
                auditSpan.setAttribute("audit.target_id", targetId);
            }

            // Tạo AuditLogEntry thành công (lấy traceId và spanId từ auditSpan hiện tại)
            AuditLogEntry entry = buildEntry(auditLog, "SUCCESS", durationMs, null, null, targetId, extraData);

            // Ghi audit log (luôn ghi, không phụ thuộc tracing)
            auditLogger.log(entry);

            // Thêm span event (chỉ khi có span đang recording)
            spanEventPublisher.publish(entry);

            auditSpan.setStatus(StatusCode.OK);
            return result;

        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - startTime;

            // Resolve SpEL (không có result khi exception)
            String targetId = resolveTargetId(auditLog, method, pjp.getArgs(), null);
            Map<String, Object> extraData = resolveExtraData(auditLog, method, pjp.getArgs(), null);

            if (targetId != null) {
                auditSpan.setAttribute("audit.target_id", targetId);
            }

            // Tạo AuditLogEntry lỗi
            AuditLogEntry entry = buildEntry(
                    auditLog,
                    "FAILURE",
                    durationMs,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    targetId,
                    extraData);

            // Ghi audit log (luôn ghi)
            auditLogger.log(entry);

            // Ghi exception lên span
            spanEventPublisher.recordException(ex);

            // Thêm span event lỗi
            spanEventPublisher.publish(entry);

            auditSpan.recordException(ex);
            auditSpan.setStatus(StatusCode.ERROR, ex.getMessage());

            // Không nuốt exception — throw lại
            throw ex;
        } finally {
            auditSpan.end();
        }
    }

    /**
     * Resolve targetId từ SpEL expression.
     */
    private String resolveTargetId(AuditLog auditLog, Method method, Object[] args, Object result) {
        String expression = auditLog.targetId();
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            EvaluationContext context = createEvaluationContext(method, args, result);
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            log.debug("Failed to resolve targetId SpEL '{}': {}", expression, e.getMessage());
            return null;
        }
    }

    /**
     * Resolve extraData từ SpEL expressions.
     * Format mỗi entry: "key=SpEL_expression"
     */
    private Map<String, Object> resolveExtraData(AuditLog auditLog, Method method, Object[] args, Object result) {
        String[] expressions = auditLog.extraData();
        if (expressions == null || expressions.length == 0) {
            return null;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        EvaluationContext context = createEvaluationContext(method, args, result);

        for (String entry : expressions) {
            try {
                int eqIndex = entry.indexOf('=');
                if (eqIndex <= 0) {
                    log.debug("Invalid extraData format (expected 'key=SpEL'): {}", entry);
                    continue;
                }
                String key = entry.substring(0, eqIndex).trim();
                String spel = entry.substring(eqIndex + 1).trim();
                Object value = parser.parseExpression(spel).getValue(context);
                if (value != null) {
                    data.put(key, value);
                }
            } catch (Exception e) {
                log.debug("Failed to resolve extraData SpEL '{}': {}", entry, e.getMessage());
            }
        }

        return data.isEmpty() ? null : data;
    }

    /**
     * Tạo SpEL EvaluationContext với method parameters và result.
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object result) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, method, args, paramNameDiscoverer);
        context.setVariable("result", result);
        context.setVariable("args", args);
        return context;
    }

    /**
     * Xây dựng AuditLogEntry từ annotation và thông tin runtime.
     */
    private AuditLogEntry buildEntry(AuditLog auditLog,
            String resultStatus,
            long durationMs,
            String errorCode,
            String errorMessage,
            String targetId,
            Map<String, Object> extraData) {

        // Lấy traceId và spanId từ span hiện tại (nullable nếu không có)
        String traceId = null;
        String spanId = null;
        try {
            Span span = Span.current();
            SpanContext ctx = span.getSpanContext();
            if (ctx.isValid()) {
                traceId = ctx.getTraceId();
                spanId = ctx.getSpanId();
            }
        } catch (Exception e) {
            log.debug("Cannot get trace/span id from current span", e);
        }

        return AuditLogEntry.builder()
                .logId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .eventType(auditLog.eventType().name())
                .message(auditLog.message())
                .severity(determineSeverity(auditLog, resultStatus))
                .actorId(actorResolver.resolveActorId())
                .actorType(actorResolver.resolveActorType())
                .requestIp(requestResolver.resolveRequestIp())
                .targetId(targetId)
                .targetType(auditLog.targetType().name())
                .serviceName(serviceName)
                .traceId(traceId)
                .spanId(spanId)
                .httpMethod(requestResolver.resolveHttpMethod())
                .endpoint(requestResolver.resolveEndpoint())
                .resultStatus(resultStatus)
                .durationMs(durationMs)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .extraData(extraData)
                .build();
    }

    /**
     * Xác định severity: nếu lỗi thì ERROR, ngược lại dùng severity từ annotation.
     */
    private String determineSeverity(AuditLog auditLog, String resultStatus) {
        if ("FAILURE".equals(resultStatus)) {
            return Severity.ERROR.name();
        }
        return auditLog.severity().name();
    }
}
