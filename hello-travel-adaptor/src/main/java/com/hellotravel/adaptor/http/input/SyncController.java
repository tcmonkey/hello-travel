package com.hellotravel.adaptor.http.input;

import com.hellotravel.adaptor.http.support.ApiViews;
import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.adaptor.http.support.HttpResults;
import com.hellotravel.application.sync.command.SyncCommand;
import com.hellotravel.application.sync.service.SyncApplication;
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

    private final ApiViews views;

    public SyncController(SyncApplication application, ApiViews views) {
        this.application = application;
        this.views = views;
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
            return views.respond(
                    application.synchronize(
                            new SyncCommand(HttpIdentity.user(httpServletRequest), after, 100)),
                    SyncResponse.class);
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
