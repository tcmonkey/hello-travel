package com.hellotravel.application.auth.service;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.application.auth.usecase.AuthActionDispatcher;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

/**
 * 认证应用入口，明确动作并将结果交给协议assembler。
 *
 * @author AIGenerator
 */
@Service
public final class AuthApplication {

    private final AuthActionDispatcher dispatcher;

    public AuthApplication(AuthActionDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * 执行指定认证用例，失败异常越过事务边界。
     *
     * @author AIGenerator
     * @param authCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<AuthResult> authenticate(AuthCommand authCommand) {
        try {
            // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
            AuthResult result = dispatcher.dispatch(authCommand);
            // 2. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
