package com.hellotravel.start.config;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 生产配置前置校验；本期仅生成，不宣称已经具备发布验证证据。
 *
 * @author AIGenerator
 */
@Configuration
@Profile("prod")
public class ProductionConfiguration {

    private final String otpHmacKey;
    private final String otpDeliveryKey;
    private final String deviceSigningKey;
    private final boolean cookieSecure;
    private final String allowedOrigins;
    private final String mailFrom;
    private final boolean smtpStartTls;
    private final boolean smtpSsl;

    public ProductionConfiguration(
            @Value("\u0024{travel.security.otp-hmac-key}") String otpHmacKey,
            @Value("\u0024{travel.security.otp-delivery-key}") String otpDeliveryKey,
            @Value("\u0024{travel.security.device-signing-key}") String deviceSigningKey,
            @Value("\u0024{travel.security.cookie-secure}") boolean cookieSecure,
            @Value("\u0024{travel.security.allowed-origins}") String allowedOrigins,
            @Value("\u0024{travel.mail.from}") String mailFrom,
            @Value("\u0024{travel.mail.smtp-starttls}") boolean smtpStartTls,
            @Value("\u0024{travel.mail.smtp-ssl}") boolean smtpSsl) {
        this.otpHmacKey = otpHmacKey;
        this.otpDeliveryKey = otpDeliveryKey;
        this.deviceSigningKey = deviceSigningKey;
        this.cookieSecure = cookieSecure;
        this.allowedOrigins = allowedOrigins;
        this.mailFrom = mailFrom;
        this.smtpStartTls = smtpStartTls;
        this.smtpSsl = smtpSsl;
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
        for (String value : java.util.List.of(otpHmacKey, otpDeliveryKey, deviceSigningKey)) {
            if (java.util.Base64.getDecoder().decode(value).length != 32 || !values.add(value)) {
                throw new IllegalStateException("security keys must be independent 32-byte values");
            }
        }
        // 3. 生产配置必须启用安全Cookie，避免明文传输认证凭据。
        if (!cookieSecure) {
            throw new IllegalStateException("production cookies require HTTPS");
        }
        // 4. 取得允许跨域的来源地址清单，供本段后续处理使用。
        // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (String origin : allowedOrigins.split(",")) {
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
        if (mailFrom != null
                && !mailFrom.isBlank()
                && !smtpStartTls
                && !smtpSsl) {
            throw new IllegalStateException("production SMTP requires TLS");
        }
    }
}
