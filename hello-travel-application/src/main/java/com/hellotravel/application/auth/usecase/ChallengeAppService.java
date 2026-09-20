package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;

import org.springframework.stereotype.Component;

/**
 * 签发邮箱验证码的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class ChallengeAppService extends BaseAuthActionAppService {

    /**
     * 注入验证码用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public ChallengeAppService(AuthActionOperations operations) {
        super("CHALLENGE", operations);
    }

    @Override
    AuthAppResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.challenge(authCommand);
    }
}
