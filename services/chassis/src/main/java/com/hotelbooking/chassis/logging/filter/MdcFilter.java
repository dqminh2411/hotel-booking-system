package com.hotelbooking.chassis.logging.filter;

import com.hotelbooking.chassis.logging.LoggingProperties;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
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

        try {
            setupMdc(request);
            // Forward traceId xuống response header
            response.setHeader(TRACE_ID_HEADER, MDC.get(MDC_TRACE_ID));

            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestCompleted(request, response, duration);

            // QUAN TRỌNG: Clear MDC sau mỗi request.
            MDC.clear();
        }
    }

    private void setupMdc(HttpServletRequest request) {
        // Lấy traceId từ OTel active span (nếu OTel SDK đang chạy)
        SpanContext otelContext = Span.current().getSpanContext();

        String traceId;
        String spanId;

        if (otelContext.isValid()) {
            traceId = otelContext.getTraceId();
            spanId  = otelContext.getSpanId();
        } else {
            traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .filter(id -> !id.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));
            spanId = UUID.randomUUID().toString().substring(0, 16);
        }

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
