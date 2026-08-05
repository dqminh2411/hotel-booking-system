package com.hotelbooking.gateway.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceHeaderFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final TextMapSetter<ServerHttpRequest.Builder> headerSetter =
            (carrier, key, value) -> {
                if (carrier != null && key != null && value != null) {
                    carrier.header(key, value);
                }
            };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Tracer tracer = GlobalOpenTelemetry.getTracer("com.hotelbooking.gateway", "1.0.0");
        ServerHttpRequest req = exchange.getRequest();
        String spanName = req.getMethod() + " " + req.getURI().getPath();

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", req.getMethod().name())
                .setAttribute("http.target", req.getURI().getPath())
                .startSpan();

        ServerHttpRequest.Builder reqBuilder = req.mutate();

        try (Scope scope = span.makeCurrent()) {
            // Inject W3C traceparent header vào request đi tới downstream microservice
            GlobalOpenTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(io.opentelemetry.context.Context.current(), reqBuilder, headerSetter);

            String traceId = span.getSpanContext().getTraceId();
            reqBuilder.header(TRACE_ID_HEADER, traceId);
            exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

            return chain.filter(exchange.mutate().request(reqBuilder.build()).build())
                    .doOnSuccess(aVoid -> {
                        int status = exchange.getResponse().getStatusCode() != null ? exchange.getResponse().getStatusCode().value() : 200;
                        span.setAttribute("http.status_code", status);
                        span.setStatus(status >= 500 ? StatusCode.ERROR : StatusCode.OK);
                    })
                    .doOnError(throwable -> {
                        span.setStatus(StatusCode.ERROR, throwable.getMessage());
                        span.recordException(throwable);
                    })
                    .doFinally(signalType -> span.end());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
