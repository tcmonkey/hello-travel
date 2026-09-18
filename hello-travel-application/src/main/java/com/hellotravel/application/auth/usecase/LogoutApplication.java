package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 撤销当前页面会话的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class LogoutApplication extends BaseAuthActionApplication {

    /**
     * 注入登出用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public LogoutApplication(AuthActionOperations operations) {
        super("LOGOUT", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.logout(authCommand);
    }
}
