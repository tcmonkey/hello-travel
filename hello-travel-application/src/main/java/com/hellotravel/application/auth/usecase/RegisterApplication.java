package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 基于注册证明创建账号的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class RegisterApplication extends BaseAuthActionApplication {

    /**
     * 注入注册用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public RegisterApplication(AuthActionOperations operations) {
        super("REGISTER", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.register(authCommand);
    }
}
