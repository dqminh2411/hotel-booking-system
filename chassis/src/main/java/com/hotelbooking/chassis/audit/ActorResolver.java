package com.hotelbooking.chassis.audit;

import io.opentelemetry.api.baggage.Baggage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lấy thông tin actor từ SecurityContext / JWT / Keycloak.
 * Nếu SecurityContext rỗng (vd: Kafka consumer thread), tự động fallback lấy từ OTel Baggage.
 */
@Slf4j
public class ActorResolver {

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTOR_TYPE_SERVICE = "SERVICE";
    private static final String ACTOR_TYPE_ANONYMOUS = "ANONYMOUS";

    /**
     * Lấy actor ID từ SecurityContext hoặc OTel Baggage.
     * Ưu tiên lấy từ JWT claim "sub" (Keycloak subject).
     * Fallback lấy từ OpenTelemetry Baggage nếu ở async/Kafka thread.
     *
     * @return actorId hoặc "anonymous" nếu không xác định được
     */
    public String resolveActorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();
                    String subject = jwt.getSubject();
                    if (subject != null && !subject.isBlank()) {
                        return subject;
                    }
                }

                String name = authentication.getName();
                if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                    return name;
                }
            }

            // Fallback: Lấy từ OpenTelemetry Baggage (cho async/Kafka thread)
            String baggageActorId = Baggage.current().getEntryValue("actorId");
            if (baggageActorId != null && !baggageActorId.isBlank()) {
                return baggageActorId;
            }

            return "anonymous";
        } catch (Throwable e) {
            log.debug("Cannot resolve actor ID from SecurityContext or Baggage", e);
            return "anonymous";
        }
    }

    /**
     * Xác định loại actor từ SecurityContext hoặc OTel Baggage.
     *
     * @return ACTOR_TYPE_USER, ACTOR_TYPE_SERVICE, hoặc ACTOR_TYPE_ANONYMOUS
     */
    public String resolveActorType() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();
                    String preferredUsername = jwt.getClaimAsString("preferred_username");
                    if (preferredUsername == null || preferredUsername.isBlank()) {
                        return ACTOR_TYPE_SERVICE;
                    }
                    return ACTOR_TYPE_USER;
                }

                String name = authentication.getName();
                if (!"anonymousUser".equals(name) && name != null && !name.isBlank()) {
                    return ACTOR_TYPE_USER;
                }
            }

            // Fallback: Lấy từ OpenTelemetry Baggage
            String baggageActorType = Baggage.current().getEntryValue("actorType");
            if (baggageActorType != null && !baggageActorType.isBlank()) {
                return baggageActorType;
            }

            return ACTOR_TYPE_ANONYMOUS;
        } catch (Throwable e) {
            log.debug("Cannot resolve actor type from SecurityContext or Baggage", e);
            return ACTOR_TYPE_ANONYMOUS;
        }
    }
}
