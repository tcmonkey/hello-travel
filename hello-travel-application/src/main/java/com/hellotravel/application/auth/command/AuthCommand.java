package com.hellotravel.application.auth.command;

/**
 * 认证业务命令；敏感字段禁止日志输出。
 *
 * @param action 服务端选择的业务动作
 * @param email 规范邮箱
 * @param password 密码，仅处理时使用
 * @param challengeId 挑战公开ID
 * @param code 验证码
 * @param purpose 验证码用途
 * @param deviceKey 服务端验证的设备随机标识
 * @param expectedSid 页面原始登录ID
 * @param accessToken 页面内存访问令牌
 * @param refreshToken HttpOnly刷新令牌
 * @param csrf CSRF随机值
 * @param rateKey 由协议层派生的频控键
 * @author AIGenerator
 */
public record AuthCommand(
        String action,
        String email,
        String password,
        String challengeId,
        String code,
        String purpose,
        String deviceKey,
        String expectedSid,
        String accessToken,
        String refreshToken,
        String csrf,
        String rateKey) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "AuthCommand{redacted}";
    }
}
