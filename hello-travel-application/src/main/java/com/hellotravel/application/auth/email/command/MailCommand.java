package com.hellotravel.application.auth.email.command;

/**
 * 验证码邮件投递命令，不写原文日志。
 *
 * @param email 收件邮箱
 * @param code 仅发送时解密的验证码
 * @param challengeId 关联验证请求
 * @author AIGenerator
 */
public record MailCommand(String email, String code, String challengeId) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "MailCommand{redacted}";
    }
}
