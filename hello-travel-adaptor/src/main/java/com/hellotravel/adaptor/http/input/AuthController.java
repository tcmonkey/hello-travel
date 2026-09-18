package com.hellotravel.adaptor.http.input;

import com.hellotravel.adaptor.http.assembler.AuthInputAssembler;
import com.hellotravel.adaptor.http.support.AuthCookies;
import com.hellotravel.adaptor.http.support.HttpResults;
import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.client.auth.request.AuthRequest;
import com.hellotravel.client.auth.response.AuthResponse;
import com.hellotravel.common.result.Result;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证HTTP入口，只负责协议编排和失败响应。
 *
 * @author AIGenerator
 */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {

    private final AuthApplication application;
    private final AuthCookies cookies;
    private final AuthInputAssembler authInputAssembler;

    public AuthController(
            AuthApplication application,
            AuthCookies cookies,
            AuthInputAssembler authInputAssembler) {
        this.application = application;
        this.cookies = cookies;
        this.authInputAssembler = authInputAssembler;
    }

    /**
     * 在完整捕获边界内完成认证协议处理。
     *
     * @param action 认证路由动作
     * @param authRequest 经过校验的认证请求
     * @param httpServletRequest HTTP请求
     * @param response HTTP响应
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    @PostMapping("/{action}")
    public Result<AuthResponse> authenticate(
            @PathVariable String action,
            @Valid @RequestBody AuthRequest authRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response) {
        try {
            // 1. 先校验路由，再取得签名设备身份并转换完整用例命令。
            String operation = authInputAssembler.action(action);
            String device = cookies.device(httpServletRequest, response);
            var command =
                    authInputAssembler.toCommand(
                            operation, authRequest, httpServletRequest, device);
            // 2. 执行应用用例，保留下层已经分类的失败结果。
            var result = application.authenticate(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 认证成功后维护刷新Cookie，再投影公开响应。
            cookies.writeSession(response, command, result.data());
            return Result.success(authInputAssembler.toResponse(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
