package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 签发邮箱验证码的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class ChallengeApplication extends BaseAuthActionApplication {

    /**
     * 注入验证码用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public ChallengeApplication(AuthActionOperations operations) {
        super("CHALLENGE", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.challenge(authCommand);
    }
}
