package com.hellotravel.application.security.assembler;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.security.command.SecurityCommand;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;

import org.springframework.stereotype.Component;

/**
 * 安全能力命令映射；用途固定字段不散落在调用链。
 *
 * @author AIGenerator
 */
@Component()
public final class SecurityCommandAssembler {

    /**
     * 固定有效Argon2证明，仅用于不存在账号的同成本验证。
     *
     * @author AIGenerator
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$argon2id$v=19$m=19456,t=2,p=1$MDEyMzQ1Njc4OWFiY2RlZg$UVh1B85rlNqd2WCQ9z89uC0mPPsTLcI90J6V3fZnMmM";

    /**
     * 构造SIGN_DEVICE能力请求，只暴露该用途实际需要的参数。
     *
     * @param key 本次转换的key快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand signDevice(String key) {
        return new SecurityCommand("SIGN_DEVICE", key, null, "device-v1");
    }

    /**
     * 构造DECRYPT能力请求，只暴露该用途实际需要的参数。
     *
     * @param encrypted 本次转换的encrypted快照
     * @param challengeId 本次转换的challengeId快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand decrypt(String encrypted, String challengeId) {
        return new SecurityCommand("DECRYPT", encrypted, null, challengeId);
    }

    /**
     * 构造HASH_PASSWORD能力请求，只暴露该用途实际需要的参数。
     *
     * @param password 本次转换的password快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand hashPassword(String password) {
        return new SecurityCommand("HASH_PASSWORD", password, null, null);
    }

    /**
     * 构造VERIFY_PASSWORD能力请求，只暴露该用途实际需要的参数。
     *
     * @param password 本次转换的password快照
     * @param hash 本次转换的hash快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand verifyPassword(String password, String hash) {
        return new SecurityCommand("VERIFY_PASSWORD", password, hash, null);
    }

    /**
     * 构造OTP能力请求，只暴露该用途实际需要的参数。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand otp() {
        return new SecurityCommand("OTP", null, null, null);
    }

    /**
     * 构造MAIL_AVAILABLE能力请求，只暴露该用途实际需要的参数。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand mailAvailable() {
        return new SecurityCommand("MAIL_AVAILABLE", null, null, null);
    }

    /**
     * 构造HMAC能力请求，只暴露该用途实际需要的参数。
     *
     * @param value 本次转换的value快照
     * @param scope 本次转换的scope快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand hmac(String value, String scope) {
        return new SecurityCommand("HMAC", value, null, scope);
    }

    /**
     * 构造ENCRYPT能力请求，只暴露该用途实际需要的参数。
     *
     * @param value 本次转换的value快照
     * @param scope 本次转换的scope快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand encrypt(String value, String scope) {
        return new SecurityCommand("ENCRYPT", value, null, scope);
    }

    /**
     * 构造LIMIT能力请求，只暴露该用途实际需要的参数。
     *
     * @param rateKey 本次转换的rateKey快照
     * @param scope 本次转换的scope快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand limit(String rateKey, String scope) {
        return new SecurityCommand("LIMIT", rateKey, null, scope);
    }

    /**
     * 根据完整登录请求和账号快照绑定密码验证，缺失账号仍执行同成本校验。
     *
     * @param command 本次转换的command快照
     * @param snapshot 本次转换的snapshot快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityCommand passwordProof(AuthCommand command, UserAccountEntity snapshot) {
        return verifyPassword(
                command.password() == null ? "" : command.password(),
                snapshot == null ? DUMMY_PASSWORD_HASH : snapshot.passwordHash());
    }
}
