package com.hellotravel.adaptor.chat.input.controller;

import com.hellotravel.adaptor.chat.input.assembler.SyncInputAssembler;
import com.hellotravel.adaptor.web.support.HttpIdentity;
import com.hellotravel.adaptor.web.support.HttpResults;
import com.hellotravel.application.chat.sync.service.SyncApplication;
import com.hellotravel.client.sync.response.SyncResponse;
import com.hellotravel.common.result.Result;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 游标补齐端点；过期或序号缺失要求重新获取全量快照。
 *
 * @author AIGenerator
 */
@RestController
public final class SyncController {

    private final SyncApplication application;
    private final SyncInputAssembler syncInputAssembler;

    public SyncController(SyncApplication application, SyncInputAssembler syncInputAssembler) {
        this.application = application;
        this.syncInputAssembler = syncInputAssembler;
    }

    /**
     * 读取当前账号连续同步事件，缺口要求重建快照。
     *
     * @author AIGenerator
     * @param after 已处理的持久事件序号
     * @param httpServletRequest HTTP请求及认证上下文
     * @return 归属和状态校验后的业务快照
     */
    @GetMapping("/api/v1/sync")
    public Result<SyncResponse> synchronize(
            @RequestParam long after, HttpServletRequest httpServletRequest) {
        try {
            // 1. 将可信用户身份和补齐游标转换为同步命令。
            var command =
                    syncInputAssembler.toCommand(HttpIdentity.user(httpServletRequest), after);
            // 2. 读取持久事件补齐结果，失败时保留稳定分类。
            var result = application.synchronize(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 投影公开事件与高水位。
            return Result.success(syncInputAssembler.toResponse(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
