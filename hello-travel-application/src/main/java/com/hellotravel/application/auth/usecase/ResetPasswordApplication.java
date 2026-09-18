package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

import org.springframework.stereotype.Component;

/**
 * 消费重置证明并吊销旧会话的认证应用用例。
 *
 * @author AIGenerator
 */
@Component
public final class ResetPasswordApplication extends BaseAuthActionApplication {

    /**
     * 注入密码重置用例使用的认证操作。
     *
     * @param operations 认证操作协作
     * @author AIGenerator
     */
    public ResetPasswordApplication(AuthActionOperations operations) {
        super("RESET", operations);
    }

    @Override
    AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations) {
        return operations.reset(authCommand);
    }
}
