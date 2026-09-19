package com.hellotravel.application.auth.security.command;

/**
 * 密码、HMAC、AEAD和频控端口输入。
 *
 * @param action 有限操作
 * @param value 输入
 * @param proof 待比对证明
 * @param scope 用途与挑战绑定范围
 * @author AIGenerator
 */
public record SecurityCommand(String action, String value, String proof, String scope) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "SecurityCommand{redacted}";
    }
}
