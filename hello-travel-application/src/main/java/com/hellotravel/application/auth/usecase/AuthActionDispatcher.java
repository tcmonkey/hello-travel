package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证动作策略注册表，只选择用例，不承载任何认证业务步骤。
 *
 * @author AIGenerator
 */
@Component
public final class AuthActionDispatcher {

    /**
     * 已冻结的认证动作策略表。
     *
     * @author AIGenerator
     */
    private final Map<String, AuthActionApplication> applications;

    /**
     * 注册全部认证动作应用并拒绝冲突动作。
     *
     * @param candidates Spring发现的动作应用
     * @author AIGenerator
     */
    public AuthActionDispatcher(List<AuthActionApplication> candidates) {
        // 1. 建立受Spring管理的认证动作到用例映射，拒绝重复动作避免启动后路由歧义。
        Map<String, AuthActionApplication> registered = new HashMap<>();
        for (AuthActionApplication candidate : candidates) {
            if (registered.putIfAbsent(candidate.action(), candidate) != null) {
                throw new IllegalStateException("duplicate auth action: " + candidate.action());
            }
        }
        // 2. 冻结注册表，避免运行时改变认证路由。
        this.applications = Map.copyOf(registered);
    }

    /**
     * 根据已受控的动作选择唯一认证用例。
     *
     * @param authCommand 已由输入层完成协议转换的命令
     * @return 对应认证用例的结果
     * @author AIGenerator
     */
    public AuthResult dispatch(AuthCommand authCommand) {
        // 1. 读取动作对应的策略，不允许未知动作落入默认业务逻辑。
        AuthActionApplication application = applications.get(authCommand.action());
        // 2. 缺少处理器时返回应用层参数错误，保护策略边界。
        if (application == null) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 3. 委托唯一用例执行，异常由应用入口统一转换。
        return application.execute(authCommand);
    }
}
