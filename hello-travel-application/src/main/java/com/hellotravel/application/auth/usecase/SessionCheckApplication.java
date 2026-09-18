package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 校验页面会话与访问令牌的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class SessionCheckApplication extends BaseAuthActionApplication {

    /**
     * 注入会话校验用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public SessionCheckApplication(AuthActionOperations operations) {
        super("CHECK", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.check(authCommand);
    }
}
