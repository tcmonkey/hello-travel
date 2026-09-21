package com.hellotravel.client.auth.response;

/**
 * 验证码投递状态的公开协议响应。
 *
 * @param challengeId 验证请求公开标识
 * @param status 当前投递状态
 * @author AIGenerator
 */
public record ChallengeStatusResponse(String challengeId, String status) {
}
