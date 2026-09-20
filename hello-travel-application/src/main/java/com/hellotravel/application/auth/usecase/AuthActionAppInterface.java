package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;

/**
 * 一个认证动作对应一个应用用例，实现由策略注册表按可信动作选择。
 *
 * @author AIGenerator
 */
public interface AuthActionAppInterface {

    /**
     * 返回本用例唯一处理的内部动作。
     *
     * @return 认证动作名称
     * @author AIGenerator
     */
    String action();

    /**
     * 执行本动作的认证应用职责。
     *
     * @param authCommand 已由输入层校验的认证命令
     * @return 本动作的认证结果
     * @author AIGenerator
     */
    AuthAppResult execute(AuthCommand authCommand);
}
