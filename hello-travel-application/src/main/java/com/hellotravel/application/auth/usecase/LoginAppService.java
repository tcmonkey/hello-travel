package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;

import org.springframework.stereotype.Component;

/**
 * 密码或验证码登录的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class LoginAppService extends BaseAuthActionAppService {

    /**
     * 注入登录用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public LoginAppService(AuthActionOperations operations) {
        super("LOGIN", operations);
    }

    @Override
    AuthAppResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.login(authCommand);
    }
}
