package com.hellotravel.start.config;

import jakarta.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * 生产配置前置校验；本期仅生成，不宣称已经具备发布验证证据。
 *
 * @author AIGenerator
 */
@Configuration
@Profile("prod")
public class ProductionConfiguration {

    private final Environment environment;

    public ProductionConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * 处理verify对应的受控业务操作。
     *
     * @author AIGenerator
     */
    @PostConstruct
    public void verify() {
        var values = new java.util.HashSet<String>();
        for (String name :
                java.util.List.of("OTP_HMAC_KEY", "OTP_DELIVERY_KEY", "DEVICE_SIGNING_KEY")) {
            String value = environment.getRequiredProperty(name);
            if (java.util.Base64.getDecoder().decode(value).length != 32 || !values.add(value)) {
                throw new IllegalStateException("security keys must be independent 32-byte values");
            }
        }
        if (!environment.getProperty("COOKIE_SECURE", Boolean.class, false)) {
            throw new IllegalStateException("production cookies require HTTPS");
        }
        String origins = environment.getRequiredProperty("ALLOWED_ORIGINS");
        for (String origin : origins.split(",")) {
            var uri = java.net.URI.create(origin);
            if (!"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || !uri.getPath().isEmpty()
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalStateException("production requires precise HTTPS origins");
            }
        }
        if (environment.containsProperty("SMTP_HOST")
                && !environment.getProperty("SMTP_STARTTLS", Boolean.class, true)) {
            throw new IllegalStateException("production SMTP requires TLS");
        }
    }
}
