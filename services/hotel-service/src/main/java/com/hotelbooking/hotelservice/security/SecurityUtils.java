package com.hotelbooking.hotelservice.security;

import com.hotelbooking.hotelservice.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Điểm truy cập DUY NHẤT tới thông tin JWT của request hiện tại.
 * Mọi Controller/Service cần biết "ai đang gọi" thì gọi qua đây,
 * thay vì tự parse SecurityContextHolder ở nhiều nơi.
 *
 * TODO xác nhận với team Auth/User Service: hiện đang dùng claim
 * chuẩn 'sub' (subject) của Keycloak làm tenantId. Nếu về sau
 * Keycloak custom thêm claim riêng (vd "tenant_id"), chỉ cần sửa
 * DUY NHẤT trong method getCurrentTenantId() bên dưới.
 */
@Component
@Slf4j
public class SecurityUtils {

    public Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AppException(
                    "UNAUTHENTICATED",
                    "Không tìm thấy thông tin xác thực hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
        return jwt;
    }

    public UUID getCurrentTenantId() {
        Jwt jwt = getCurrentJwt();
        String subject = jwt.getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            log.error("Claim 'sub' trong JWT không phải UUID hợp lệ: {}", subject);
            throw new AppException(
                    "INVALID_TOKEN_CLAIM",
                    "Token không chứa tenantId hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
