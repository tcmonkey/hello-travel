package com.hellotravel.application.auth.result;

/**
 * 应用认证结果，协议转换不得输出内部主键或刷新令牌。
 *
 * @param userId 内部身份，仅服务端使用
 * @param userPublicId 公开账号ID
 * @param email 邮箱
 * @param sid 页面会话ID
 * @param sessionId 服务端登录主键
 * @param accessToken 内存访问令牌
 * @param refreshToken 只交HttpOnly Cookie
 * @param csrf 跨站校验值
 * @param challengeId 受理验证码挑战ID
 * @author AIGenerator
 */
public record AuthResult(
        Long userId,
        String userPublicId,
        String email,
        String sid,
        Long sessionId,
        String accessToken,
        String refreshToken,
        String csrf,
        String challengeId) {
        }
