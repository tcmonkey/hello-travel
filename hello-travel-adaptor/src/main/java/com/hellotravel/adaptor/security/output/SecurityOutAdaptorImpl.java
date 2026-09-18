package com.hellotravel.adaptor.security.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.security.output.converter.SecurityOutputConverter;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.command.SecurityCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.security.SecurityDO;

import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Argon2id、用途HMAC、AES-GCM与Redis原子限流；缺配置不降级固定码。
 *
 * @author AIGenerator
 */
@Component
public final class SecurityOutAdaptorImpl implements SecurityOutAdaptor {

    private final Environment environment;
    private final StringRedisTemplate redis;
    private final SecurityOutputConverter securityOutputConverter;

    public SecurityOutAdaptorImpl(
            Environment environment,
            StringRedisTemplate redis,
            SecurityOutputConverter securityOutputConverter) {
        this.environment = environment;
        this.redis = redis;
        this.securityOutputConverter = securityOutputConverter;
    }

    /**
     * 处理process对应的受控业务操作。
     *
     * @author AIGenerator
     * @param securityCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<SecurityDO> process(SecurityCommand securityCommand) {
        try {
            // 1. 按已限定的安全动作分发校验、加密和限流；本入口统一捕获失败。
            return Result.success(
                    switch (securityCommand.action()) {
                        case "HASH_PASSWORD" ->
                                securityOutputConverter.value(
                                        encoder().encode(securityCommand.value()));
                        case "VERIFY_PASSWORD" ->
                                securityOutputConverter.verification(
                                        encoder()
                                                .matches(
                                                        securityCommand.value(),
                                                        securityCommand.proof()));
                        case "OTP" ->
                                securityOutputConverter.value(
                                        String.format(
                                                java.util.Locale.ROOT,
                                                "%06d",
                                                new SecureRandom().nextInt(1000000)));
                        case "HMAC" ->
                                securityOutputConverter.value(
                                        Base64.getEncoder()
                                                .encodeToString(
                                                        hmac(
                                                                securityCommand.value(),
                                                                securityCommand.scope(),
                                                                "OTP_HMAC_KEY")));
                        case "SIGN_DEVICE" ->
                                securityOutputConverter.value(
                                        Base64.getUrlEncoder()
                                                .withoutPadding()
                                                .encodeToString(
                                                        hmac(
                                                                securityCommand.value(),
                                                                "device",
                                                                "DEVICE_SIGNING_KEY")));
                        case "ENCRYPT" ->
                                securityOutputConverter.value(
                                        encrypt(securityCommand.value(), securityCommand.scope()));
                        case "DECRYPT" ->
                                securityOutputConverter.value(
                                        decrypt(securityCommand.value(), securityCommand.scope()));
                        case "MAIL_AVAILABLE" ->
                                securityOutputConverter.verification(
                                        environment.containsProperty("SMTP_HOST")
                                                && environment.containsProperty("SMTP_FROM"));
                        case "LIMIT" ->
                                securityOutputConverter.verification(
                                        limit(securityCommand.value(), securityCommand.scope()));
                        default -> throw new IllegalArgumentException("security action");
                    });
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    private Argon2PasswordEncoder encoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    private byte[] key(String name) {
        // 1. 取得本次预算或解析后的业务值，供本段后续处理使用。
        String value = environment.getProperty(name);
        // 2. 缺少安全操作输入时拒绝计算，不将空值作为有效凭据。
        if (value == null) {
            throw new IllegalStateException("missing security key");
        }
        // 3. 准备当前操作的存储或签名标识。
        byte[] key = Base64.getDecoder().decode(value);
        // 4. 依据格式、长度或数量边界处理分支，避免继续使用无效数据。
        if (key.length != 32) {
            throw new IllegalStateException("key size");
        }
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return key;
    }

    private byte[] hmac(String value, String scope, String keyName) throws Exception {
        // 1. 取得用于生成不可逆证明的认证码实例，供本段后续处理使用。
        Mac mac = Mac.getInstance("HmacSHA256");
        // 2. 执行init职责步骤，并把失败交给所属事务或入口处理。
        mac.init(new SecretKeySpec(key(keyName), "HmacSHA256"));
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return mac.doFinal((scope + "|" + value).getBytes(StandardCharsets.UTF_8));
    }

    private String encrypt(String value, String scope) throws Exception {
        // 1. 取得本次加密的随机数，供本段后续处理使用。
        byte[] nonce = new byte[12];
        // 2. 执行nextBytes职责步骤，并把失败交给所属事务或入口处理。
        new SecureRandom().nextBytes(nonce);
        // 3. 取得用于保护私有载荷的加密实例，供本段后续处理使用。
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // 4. 执行init职责步骤，并把失败交给所属事务或入口处理。
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key("OTP_DELIVERY_KEY"), "AES"),
                new GCMParameterSpec(128, nonce));
        // 5. 执行updateAAD职责步骤，并把失败交给所属事务或入口处理。
        cipher.updateAAD(scope.getBytes(StandardCharsets.UTF_8));
        // 6. 准备可投递的加密验证码，原始码不进入数据库。
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        // 7. 返回本段实际处理结果，保持本层输出契约。
        return Base64.getEncoder()
                .encodeToString(
                        ByteBuffer.allocate(12 + encrypted.length)
                                .put(nonce)
                                .put(encrypted)
                                .array());
    }

    private String decrypt(String value, String scope) throws Exception {
        // 1. 取得待校验的上传文件字节，供本段后续处理使用。
        byte[] bytes = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // 2. 执行init职责步骤，并把失败交给所属事务或入口处理。
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key("OTP_DELIVERY_KEY"), "AES"),
                new GCMParameterSpec(128, bytes, 0, 12));
        // 3. 执行updateAAD职责步骤，并把失败交给所属事务或入口处理。
        cipher.updateAAD(scope.getBytes(StandardCharsets.UTF_8));
        // 4. 返回本段实际处理结果，保持本层输出契约。
        return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
    }

    private boolean limit(String ip, String scope) {
        // 1. 取得服务商返回的失败字段，供本段后续处理使用。
        boolean issue = scope.startsWith("issue:");
        String key =
                "hello-travel:rate:"
                        + java.util.HexFormat.of()
                                .formatHex(Ids.hash(scope + "|" + (issue ? "" : ip)));
        String script =
                "local n=redis.call('INCR',KEYS[1]); if n==1 then"
                        + " redis.call('EXPIRE',KEYS[1],ARGV[1]); end; return n";
        Long n =
                redis.execute(
                        new DefaultRedisScript<>(script, Long.class),
                        List.of(key),
                        issue ? "60" : "300");
        // 2. 仅在Redis返回有效计数时设置首次请求的限流过期时间。
        if (n == null) {
            throw new IllegalStateException("limiter unavailable");
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return n <= (issue ? 1 : 10);
    }
}
