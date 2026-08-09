package com.hotelbooking.chassis.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import io.opentelemetry.api.baggage.Baggage;

/**
 * Lấy thông tin request hiện tại.
 *
 * Bao gồm: IP, endpoint, HTTP method.
 * Không để Aspect thao tác trực tiếp với HttpServletRequest.
 */
@Slf4j
public class RequestResolver {

    /**
     * Lấy IP client.
     * Hỗ trợ X-Forwarded-For và X-Real-IP cho trường hợp đứng sau reverse proxy.
     * Fallback lấy từ OpenTelemetry Baggage nếu ở async/Kafka thread.
     *
     * @return IP hoặc "unknown" nếu không xác định được
     */
    public String resolveRequestIp() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String ip = extractClientIp(request);
                if (ip != null && !ip.isBlank()) {
                    return ip;
                }
            }

            // Fallback: Lấy từ OpenTelemetry Baggage (cho async/Kafka thread)
            String baggageIp = Baggage.current().getEntryValue("requestIp");
            if (baggageIp != null && !baggageIp.isBlank()) {
                return baggageIp;
            }

            return "unknown";
        } catch (Exception e) {
            log.debug("Cannot resolve request IP from HttpServletRequest or Baggage", e);
            return "unknown";
        }
    }

    /**
     * Trích xuất client IP, xử lý trường hợp đứng sau reverse proxy / load balancer.
     */
    private String extractClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(xff -> xff.split(",")[0].trim())
                .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                        .orElse(request.getRemoteAddr()));
    }

    /**
     * Lấy endpoint (URI) của request hiện tại.
     *
     * @return endpoint hoặc null nếu không trong HTTP context
     */
    public String resolveEndpoint() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            return request.getRequestURI();
        } catch (Exception e) {
            log.debug("Cannot resolve endpoint", e);
            return null;
        }
    }

    /**
     * Lấy HTTP method của request hiện tại.
     *
     * @return HTTP method hoặc null nếu không trong HTTP context
     */
    public String resolveHttpMethod() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                return null;
            }
            return request.getMethod();
        } catch (Exception e) {
            log.debug("Cannot resolve HTTP method", e);
            return null;
        }
    }

    /**
     * Lấy HttpServletRequest từ RequestContextHolder.
     *
     * @return HttpServletRequest hoặc null nếu không trong Servlet context
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    
}
