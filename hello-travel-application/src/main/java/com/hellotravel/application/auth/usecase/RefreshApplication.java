package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 轮换刷新令牌与页面会话凭据的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class RefreshApplication extends BaseAuthActionApplication {

    /**
     * 注入刷新用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public RefreshApplication(AuthActionOperations operations) {
        super("REFRESH", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.refresh(authCommand);
    }
}
