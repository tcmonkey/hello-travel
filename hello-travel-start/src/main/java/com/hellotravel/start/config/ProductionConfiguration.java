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
        // 1. 取得待序列化的上下文用量字段，供本段后续处理使用。
        var values = new java.util.HashSet<String>();
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (String name :
                java.util.List.of("OTP_HMAC_KEY", "OTP_DELIVERY_KEY", "DEVICE_SIGNING_KEY")) {
            String value = environment.getRequiredProperty(name);
            if (java.util.Base64.getDecoder().decode(value).length != 32 || !values.add(value)) {
                throw new IllegalStateException("security keys must be independent 32-byte values");
            }
        }
        // 3. 生产配置必须启用安全Cookie，避免明文传输认证凭据。
        if (!environment.getProperty("COOKIE_SECURE", Boolean.class, false)) {
            throw new IllegalStateException("production cookies require HTTPS");
        }
        // 4. 取得允许跨域的来源地址清单，供本段后续处理使用。
        String origins = environment.getRequiredProperty("ALLOWED_ORIGINS");
        // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
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
        // 6. 已配置SMTP时必须启用STARTTLS或隐式TLS，拒绝不安全生产配置。
        if (environment.containsProperty("SMTP_HOST")
                && !environment.getProperty("SMTP_STARTTLS", Boolean.class, true)
                && !environment.getProperty("SMTP_SSL", Boolean.class, false)) {
            throw new IllegalStateException("production SMTP requires TLS");
        }
    }
}
