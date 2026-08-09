package com.hotelbooking.chassis.audit;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter tự động trích xuất thông tin actorId, actorType, requestIp từ HTTP Request
 * và đưa vào OpenTelemetry Baggage context.
 */
@RequiredArgsConstructor
public class AuditBaggageFilter extends OncePerRequestFilter {

    private final ActorResolver actorResolver;
    private final RequestResolver requestResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String actorId = actorResolver.resolveActorId();
        String actorType = actorResolver.resolveActorType();
        String requestIp = requestResolver.resolveRequestIp();

        BaggageBuilder builder = Baggage.current().toBuilder();
        if (actorId != null && !"anonymous".equals(actorId)) {
            builder.put("actorId", actorId);
        }
        if (actorType != null && !"ANONYMOUS".equals(actorType)) {
            builder.put("actorType", actorType);
        }
        if (requestIp != null && !"unknown".equals(requestIp)) {
            builder.put("requestIp", requestIp);
        }

        Baggage baggage = builder.build();
        try (Scope scope = baggage.makeCurrent()) {
            filterChain.doFilter(request, response);
        }
    }
}
