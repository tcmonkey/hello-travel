package com.hellotravel.adaptor.auth.input;

import com.hellotravel.adaptor.auth.input.assembler.AuthAssembler;
import com.hellotravel.adaptor.common.AuthCookies;
import com.hellotravel.adaptor.common.HttpResults;
import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.application.auth.ChallengeStatusAppService;
import com.hellotravel.client.auth.request.AuthRequest;
import com.hellotravel.client.auth.response.AuthResponse;
import com.hellotravel.client.auth.response.ChallengeStatusResponse;
import com.hellotravel.common.result.Result;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final AuthAppService application;
    private final ChallengeStatusAppService challengeStatusApplication;
    private final AuthCookies cookies;
    private final AuthAssembler authAssembler;

    public AuthController(
            AuthAppService application,
            ChallengeStatusAppService challengeStatusApplication,
            AuthCookies cookies,
            AuthAssembler authAssembler) {
        this.application = application;
        this.challengeStatusApplication = challengeStatusApplication;
        this.cookies = cookies;
        this.authAssembler = authAssembler;
    }

    /**
     * 查询验证码投递状态，前端据此决定是否可以提交邮箱证明。
     *
     * @param challengeId 验证请求公开标识
     * @return 不含敏感字段的投递状态
     * @author AIGenerator
     */
    @GetMapping("/challenge/{challengeId}/status")
    public Result<ChallengeStatusResponse> challengeStatus(@PathVariable String challengeId) {
        try {
            // 1. 查询当前验证码投递状态，应用层负责校验公开标识和读取边界。
            var result = challengeStatusApplication.query(challengeId);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 2. 投影公开状态响应，不向浏览器返回邮箱和验证码内容。
            return Result.success(authAssembler.toChallengeStatus(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
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
            String operation = authAssembler.action(action);
            String device = cookies.device(httpServletRequest, response);
            var command =
                    authAssembler.toCommand(
                            operation, authRequest, httpServletRequest, device);
            // 2. 执行应用用例，保留下层已经分类的失败结果。
            var result = application.authenticate(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 认证成功后维护刷新Cookie，再投影公开响应。
            cookies.writeSession(response, command, result.data());
            return Result.success(authAssembler.toResponse(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
