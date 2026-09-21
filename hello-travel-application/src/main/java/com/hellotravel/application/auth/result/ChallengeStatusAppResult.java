package com.hellotravel.application.auth.result;

/**
 * 邮箱验证码投递状态的应用层结果。
 *
 * @param challengeId 验证请求公开标识
 * @param status 当前投递状态
 * @author AIGenerator
 */
public record ChallengeStatusAppResult(String challengeId, String status) {
}
