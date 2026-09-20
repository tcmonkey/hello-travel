package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;

import org.springframework.stereotype.Component;

/**
 * 校验页面会话与访问令牌的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class SessionCheckAppService extends BaseAuthActionAppService {

    /**
     * 注入会话校验用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public SessionCheckAppService(AuthActionOperations operations) {
        super("CHECK", operations);
    }

    @Override
    AuthAppResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.check(authCommand);
    }
}
