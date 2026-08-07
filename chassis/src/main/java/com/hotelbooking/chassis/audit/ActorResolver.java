package com.hotelbooking.chassis.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lấy thông tin actor từ SecurityContext / JWT / Keycloak.
 *
 * Trả về actorId và actorType.
 * Không để Aspect thao tác trực tiếp với SecurityContext.
 * 
 */
@Slf4j
public class ActorResolver {

    private static final String ACTOR_TYPE_USER = "USER";
    private static final String ACTOR_TYPE_SERVICE = "SERVICE";
    private static final String ACTOR_TYPE_ANONYMOUS = "ANONYMOUS";

    /**
     * Lấy actor ID từ SecurityContext.
     * Ưu tiên lấy từ JWT claim "sub" (Keycloak subject).
     *
     * @return actorId hoặc "anonymous" nếu không xác định được
     */
    public String resolveActorId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "anonymous";
            }

            // Keycloak JWT: lấy subject (user id)
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                String subject = jwt.getSubject();
                if (subject != null && !subject.isBlank()) {
                    return subject;
                }
            }

            // Fallback: lấy từ authentication name
            String name = authentication.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
                return name;
            }

            return "anonymous";
        } catch (Throwable e) {
            log.debug("Cannot resolve actor ID from SecurityContext", e);
            return "anonymous";
        }
    }

    /**
     * Xác định loại actor.
     *
     * @return ACTOR_TYPE_USER, ACTOR_TYPE_SERVICE, hoặc ACTOR_TYPE_ANONYMOUS
     */
    public String resolveActorType() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ACTOR_TYPE_ANONYMOUS;
            }

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                // Keycloak client credentials flow: azp là client_id, không có
                // preferred_username
                String preferredUsername = jwt.getClaimAsString("preferred_username");
                if (preferredUsername == null || preferredUsername.isBlank()) {
                    return ACTOR_TYPE_SERVICE;
                }
                return ACTOR_TYPE_USER;
            }

            String name = authentication.getName();
            if ("anonymousUser".equals(name) || name == null || name.isBlank()) {
                return ACTOR_TYPE_ANONYMOUS;
            }

            return ACTOR_TYPE_USER;
        } catch (Throwable e) {
            log.debug("Cannot resolve actor type from SecurityContext", e);
            return ACTOR_TYPE_ANONYMOUS;
        }
    }
}
