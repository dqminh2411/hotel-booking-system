package com.hotelbooking.chassis.logging.filter;

import com.hotelbooking.chassis.logging.LoggingProperties;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)  // Chạy trước mọi filter khác
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    // Header names — phải đồng nhất toàn hệ thống
    public static final String TRACE_ID_HEADER  = "X-Trace-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER   = "X-User-Id";

    // MDC key names — phải khớp với field names trong logback-spring.xml
    private static final String MDC_TRACE_ID   = "traceId";
    private static final String MDC_SPAN_ID    = "spanId";
    private static final String MDC_TENANT_ID  = "tenantId";
    private static final String MDC_USER_ID    = "userId";
    private static final String MDC_CLIENT_IP  = "clientIp";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_HTTP_METHOD = "http.method";
    private static final String MDC_HTTP_PATH   = "http.path";

    private static final TextMapGetter<HttpServletRequest> HTTP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    private final LoggingProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (properties.getExcludePaths() == null) return false;
        return properties.getExcludePaths().stream()
            .anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
                                    throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        // 1. Trích xuất context từ HTTP Header (W3C traceparent hoặc X-Trace-Id nếu có)
        Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, HTTP_GETTER);

        // 2. Tạo OTel Server Span cho HTTP Request
        Tracer tracer = GlobalOpenTelemetry.getTracer("com.hotelbooking.chassis", "1.1.0");
        String spanName = request.getMethod() + " " + request.getRequestURI();

        Span span = tracer.spanBuilder(spanName)
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", request.getMethod())
                .setAttribute("http.target", request.getRequestURI())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            setupMdc(request, span);
            // Forward traceId xuống response header
            response.setHeader(TRACE_ID_HEADER, MDC.get(MDC_TRACE_ID));

            chain.doFilter(request, response);

            span.setAttribute("http.status_code", response.getStatus());
            if (response.getStatus() >= 500) {
                span.setStatus(StatusCode.ERROR, "HTTP " + response.getStatus());
            } else {
                span.setStatus(StatusCode.OK);
            }

        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestCompleted(request, response, duration);

            span.end(); // Gửi span sang BatchSpanProcessor -> OTel Collector -> Jaeger
            MDC.clear();
        }
    }

    private void setupMdc(HttpServletRequest request, Span span) {
        String traceId = span.getSpanContext().getTraceId();
        String spanId  = span.getSpanContext().getSpanId();

        MDC.put(MDC_TRACE_ID,   traceId);
        MDC.put(MDC_SPAN_ID,    spanId);
        MDC.put(MDC_TENANT_ID,  getHeaderOrDefault(request, TENANT_ID_HEADER, "platform"));
        MDC.put(MDC_USER_ID,    getHeaderOrDefault(request, USER_ID_HEADER, "anonymous"));
        MDC.put(MDC_CLIENT_IP,  extractClientIp(request));
        MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_HTTP_PATH,   request.getRequestURI());
    }

    private void logRequestCompleted(HttpServletRequest request,
                                     HttpServletResponse response,
                                     long durationMs) {
        int status = response.getStatus();

        // Slow request warning
        if (durationMs > properties.getSlowRequestThresholdMs()) {
            log.warn("Slow request detected",
                StructuredArguments.kv("event", "SLOW_REQUEST"),
                StructuredArguments.kv("duration_ms", durationMs),
                StructuredArguments.kv("threshold_ms", properties.getSlowRequestThresholdMs()));
        }

        if (status >= 500) {
            log.error("HTTP request completed with server error",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        } else if (status >= 400) {
            log.warn("HTTP request completed with client error",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        } else {
            log.info("HTTP request completed",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        // Xử lý trường hợp đứng sau reverse proxy / load balancer
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .map(xff -> xff.split(",")[0].trim())
            .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .orElse(request.getRemoteAddr()));
    }

    private String getHeaderOrDefault(HttpServletRequest request,
                                      String headerName, String defaultValue) {
        return Optional.ofNullable(request.getHeader(headerName))
            .filter(v -> !v.isBlank())
            .orElse(defaultValue);
    }
}
