package com.hotelbooking.chassis.logging;

import com.hotelbooking.chassis.logging.aop.LoggingAspect;
import com.hotelbooking.chassis.logging.filter.MdcFilter;
import com.hotelbooking.chassis.logging.util.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
    name = "chassis.logging.enabled",
    havingValue = "true",
    matchIfMissing = true   // Mặc định bật nếu không cấu hình
)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    @Bean
    public SensitiveDataMasker sensitiveDataMasker(LoggingProperties properties) {
        return new SensitiveDataMasker(properties);
    }

    /**
     * MdcFilter: chỉ tạo nếu là web application (có Servlet)
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public MdcFilter mdcFilter(LoggingProperties properties) {
        return new MdcFilter(properties);
    }

    /**
     * LoggingAspect: chỉ tạo nếu AOP được bật trong properties
     */
    @Bean
    @ConditionalOnProperty(
        name = "chassis.logging.aspect-enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public LoggingAspect loggingAspect(SensitiveDataMasker masker) {
        return new LoggingAspect(masker);
    }
}
