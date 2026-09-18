package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;

/**
 * 认证动作应用的公共策略骨架，统一动作标识与委托契约。
 *
 * @author AIGenerator
 */
abstract class BaseAuthActionApplication implements AuthActionApplication {

    /**
     * 本策略负责的内部认证动作。
     *
     * @author AIGenerator
     */
    private final String action;

    /**
     * 被动作应用复用的认证操作协作。
     *
     * @author AIGenerator
     */
    private final AuthActionOperations operations;

    BaseAuthActionApplication(String action, AuthActionOperations operations) {
        this.action = action;
        this.operations = operations;
    }

    /**
     * 返回本策略唯一处理的认证动作。
     *
     * @return 认证动作名称
     * @author AIGenerator
     */
    @Override
    public final String action() {
        return action;
    }

    /**
     * 执行当前动作的具体认证用例。
     *
     * @param authCommand 已校验的认证命令
     * @return 当前认证动作结果
     * @author AIGenerator
     */
    @Override
    public final AuthResult execute(AuthCommand authCommand) {
        return executeAction(authCommand, operations);
    }

    abstract AuthResult executeAction(AuthCommand authCommand, AuthActionOperations operations);
}
