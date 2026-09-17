package com.hellotravel.client.auth.response;

/**
 * 认证结果；刷新令牌只放HttpOnly Cookie。
 *
 * @param userId 用户公开标识
 * @param email 邮箱
 * @param sid 页面绑定的登录标识
 * @param accessToken 页面内存访问令牌
 * @param csrf 页面CSRF值
 * @param challengeId 验证请求编号
 * @author AIGenerator
 */
public record AuthResponse(
        String userId,
        String email,
        String sid,
        String accessToken,
        String csrf,
        String challengeId) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "AuthResponse{redacted}";
    }
}
