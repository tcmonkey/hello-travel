package com.hellotravel.client.auth.request;

/**
 * 认证请求，原文禁止日志输出。
 *
 * @param email 邮箱
 * @param password 账号密码
 * @param challengeId 验证请求编号
 * @param code 邮箱验证码
 * @param purpose 验证用途
 * @author AIGenerator
 */
public record AuthRequest(
        String email, String password, String challengeId, String code, String purpose) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "AuthRequest{redacted}";
    }
}
