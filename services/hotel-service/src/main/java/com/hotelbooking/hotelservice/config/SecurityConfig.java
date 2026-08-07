package com.hotelbooking.hotelservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    // Chỉ những path/method thực sự công khai (khách chưa đăng nhập vẫn xem được)
    String[] publicGetPatterns = {
            "/api/hotels/**",        // GET chi tiết hotel, search, room-types... vẫn public
            "/api/room-types/**"
    };

    String[] publicAnyMethod = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/health",
            "/actuator/**",
            "/api/minio/**"          // TODO: xoá whitelist này khi MinioTestController bị gỡ khỏi production
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.GET, publicGetPatterns).permitAll()
                        .requestMatchers(publicAnyMethod).permitAll()
                        // Role/ownership cụ thể (HOTEL_OWNER, PLATFORM_ADMIN, đúng chủ hotel...)
                        // được kiểm tra chi tiết bằng @PreAuthorize + ownership-check ở Service,
                        // ở đây chỉ yêu cầu bắt buộc phải có JWT hợp lệ.
                        .requestMatchers(HttpMethod.POST, "/api/hotels").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/hotels/*/images").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/hotels/*/images/*").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    /**
     * Converter trích xuất các role từ claim 'realm_access.roles' trong Keycloak JWT
     * và chuyển thành các GrantedAuthority dạng "ROLE_<ROLE_NAME>" cho Spring Security.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(defaultGrantedAuthoritiesConverter.convert(jwt));

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String roleName) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }
                }
            }
            return authorities;
        });

        return jwtAuthenticationConverter;
    }
}
