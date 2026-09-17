package com.hellotravel.adaptor.security.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.command.SecurityCommand;
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

    public SecurityOutAdaptorImpl(Environment environment, StringRedisTemplate redis) {
        this.environment = environment;
        this.redis = redis;
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
            return Result.success(
                    switch (securityCommand.action()) {
                        case "HASH_PASSWORD" ->
                                new SecurityDO(encoder().encode(securityCommand.value()), true);
                        case "VERIFY_PASSWORD" ->
                                new SecurityDO(
                                        null,
                                        encoder()
                                                .matches(
                                                        securityCommand.value(),
                                                        securityCommand.proof()));
                        case "OTP" ->
                                new SecurityDO(
                                        String.format(
                                                java.util.Locale.ROOT,
                                                "%06d",
                                                new SecureRandom().nextInt(1000000)),
                                        true);
                        case "HMAC" ->
                                new SecurityDO(
                                        Base64.getEncoder()
                                                .encodeToString(
                                                        hmac(
                                                                securityCommand.value(),
                                                                securityCommand.scope(),
                                                                "OTP_HMAC_KEY")),
                                        true);
                        case "SIGN_DEVICE" ->
                                new SecurityDO(
                                        Base64.getUrlEncoder()
                                                .withoutPadding()
                                                .encodeToString(
                                                        hmac(
                                                                securityCommand.value(),
                                                                "device",
                                                                "DEVICE_SIGNING_KEY")),
                                        true);
                        case "ENCRYPT" ->
                                new SecurityDO(
                                        encrypt(securityCommand.value(), securityCommand.scope()),
                                        true);
                        case "DECRYPT" ->
                                new SecurityDO(
                                        decrypt(securityCommand.value(), securityCommand.scope()),
                                        true);
                        case "MAIL_AVAILABLE" ->
                                new SecurityDO(
                                        null,
                                        environment.containsProperty("SMTP_HOST")
                                                && environment.containsProperty("SMTP_FROM"));
                        case "LIMIT" ->
                                new SecurityDO(
                                        null,
                                        limit(securityCommand.value(), securityCommand.scope()));
                        default -> throw new IllegalArgumentException("security action");
                    });
        } catch (Exception exception) {
            return Result.failure(AdaptorErrorCode.UNAVAILABLE);
        }
    }

    private Argon2PasswordEncoder encoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    private byte[] key(String name) {
        String value = environment.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("missing security key");
        }
        byte[] key = Base64.getDecoder().decode(value);
        if (key.length != 32) {
            throw new IllegalStateException("key size");
        }
        return key;
    }

    private byte[] hmac(String value, String scope, String keyName) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key(keyName), "HmacSHA256"));
        return mac.doFinal((scope + "|" + value).getBytes(StandardCharsets.UTF_8));
    }

    private String encrypt(String value, String scope) throws Exception {
        byte[] nonce = new byte[12];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key("OTP_DELIVERY_KEY"), "AES"),
                new GCMParameterSpec(128, nonce));
        cipher.updateAAD(scope.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder()
                .encodeToString(
                        ByteBuffer.allocate(12 + encrypted.length)
                                .put(nonce)
                                .put(encrypted)
                                .array());
    }

    private String decrypt(String value, String scope) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key("OTP_DELIVERY_KEY"), "AES"),
                new GCMParameterSpec(128, bytes, 0, 12));
        cipher.updateAAD(scope.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
    }

    private boolean limit(String ip, String scope) {
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
        if (n == null) {
            throw new IllegalStateException("limiter unavailable");
        }
        return n <= (issue ? 1 : 10);
    }
}
