package com.hotelbooking.chassis.logging.kafka;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class KafkaLoggingUtils {

    public static final String HEADER_TRACE_ID  = "X-Trace-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID   = "X-User-Id";

    private static final TextMapGetter<Headers> KAFKA_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return StreamSupport.stream(headers.spliterator(), false)
                .map(Header::key)
                .collect(Collectors.toList());
        }

        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            return header != null
                ? new String(header.value(), StandardCharsets.UTF_8)
                : null;
        }
    };

    private KafkaLoggingUtils() {}

    /**
     * Restore OTel context từ Kafka header và setup MDC.
     */
    public static Scope setupFromRecord(ConsumerRecord<?, ?> record) {
        try {
            Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), KAFKA_GETTER);

            Scope scope = extractedContext.makeCurrent();

            SpanContext spanCtx = Span.current().getSpanContext();
            if (spanCtx.isValid()) {
                MDC.put("traceId", spanCtx.getTraceId());
                MDC.put("spanId",  spanCtx.getSpanId());
            } else {
                setupMdcFromRecord(record);
            }

            MDC.put("tenantId",        extractHeader(record, HEADER_TENANT_ID).orElse("platform"));
            MDC.put("userId",          extractHeader(record, HEADER_USER_ID).orElse("system"));
            MDC.put("kafka.topic",     record.topic());
            MDC.put("kafka.partition", String.valueOf(record.partition()));
            MDC.put("kafka.offset",    String.valueOf(record.offset()));

            return scope;
        } catch (Exception e) {
            return Scope.noop();
        }
    }

    /**
     * Gọi ở đầu mỗi @KafkaListener để setup MDC từ Kafka message header.
     */
    public static void setupMdcFromRecord(ConsumerRecord<?, ?> record) {
        try {
            MDC.put("traceId",  extractHeader(record, HEADER_TRACE_ID)
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", "")));
            MDC.put("tenantId", extractHeader(record, HEADER_TENANT_ID)
                .orElse("platform"));
            MDC.put("userId",   extractHeader(record, HEADER_USER_ID)
                .orElse("system"));

            // Kafka-specific context
            MDC.put("kafka.topic",     record.topic());
            MDC.put("kafka.partition", String.valueOf(record.partition()));
            MDC.put("kafka.offset",    String.valueOf(record.offset()));
            MDC.put("kafka.key",       record.key() != null ? String.valueOf(record.key()) : "");
        } catch (Exception e) {
            // Không để lỗi setup MDC làm hỏng business logic
        }
    }

    /**
     * Xóa MDC sau khi xử lý xong Kafka message.
     */
    public static void clearMdc() {
        MDC.clear();
    }

    private static Optional<String> extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) return Optional.empty();
        String value = new String(header.value(), StandardCharsets.UTF_8);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
