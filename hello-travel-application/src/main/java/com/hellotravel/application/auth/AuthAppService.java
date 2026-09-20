package com.hellotravel.application.auth;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.auth.usecase.AuthActionAppInterface;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证应用入口，明确动作并将结果交给协议assembler。
 *
 * @author AIGenerator
 */
@Service
public final class AuthAppService {

    /**
     * 已冻结的认证动作策略表。
     *
     * @author AIGenerator
     */
    private final Map<String, AuthActionAppInterface> applications;

    /**
     * 注册全部认证动作应用并拒绝冲突动作。
     *
     * @param candidates Spring发现的动作应用
     * @author AIGenerator
     */
    public AuthAppService(List<AuthActionAppInterface> candidates) {
        // 1. 建立受Spring管理的认证动作到用例映射，拒绝重复动作避免启动后路由歧义。
        Map<String, AuthActionAppInterface> registered = new HashMap<>();
        for (AuthActionAppInterface candidate : candidates) {
            if (registered.putIfAbsent(candidate.action(), candidate) != null) {
                throw new IllegalStateException("duplicate auth action: " + candidate.action());
            }
        }
        // 2. 冻结注册表，避免运行时改变认证路由。
        this.applications = Map.copyOf(registered);
    }

    /**
     * 执行指定认证用例，失败异常越过事务边界。
     *
     * @author AIGenerator
     * @param authCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<AuthAppResult> authenticate(AuthCommand authCommand) {
        try {
            // 1. 读取动作对应的策略，不允许未知动作落入默认业务逻辑。
            AuthActionAppInterface application = applications.get(authCommand.action());
            // 2. 缺少处理器时返回应用层参数错误，保护策略边界。
            if (application == null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 3. 委托唯一用例执行，异常由应用入口统一转换。
            AuthAppResult result = application.execute(authCommand);
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
