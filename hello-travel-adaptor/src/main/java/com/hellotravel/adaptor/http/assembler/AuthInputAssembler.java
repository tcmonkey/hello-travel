package com.hellotravel.adaptor.http.assembler;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.adaptor.http.support.HttpProtocol;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.client.auth.request.AuthRequest;
import com.hellotravel.client.auth.response.AuthResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * 认证协议双向映射，业务判断保留在应用及领域。
 *
 * @author AIGenerator
 */
@Component()
public final class AuthInputAssembler {

    /**
     * 绑定认证请求和可信HTTP上下文，原始刷新令牌只来自Cookie。
     *
     * @param operation 已解释的认证用例动作
     * @param request 本次转换的request快照
     * @param http 本次转换的http快照
     * @param device 本次转换的device快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthCommand toCommand(
            String operation, AuthRequest request, HttpServletRequest http, String device) {
        // 1. 整体映射已解释的动作、请求及凭据，内部主键不由客户端指定。
        return new AuthCommand(
                operation,
                request.email(),
                request.password(),
                request.challengeId(),
                request.code(),
                request.purpose(),
                device,
                http.getHeader(HttpProtocol.SESSION_HEADER),
                HttpIdentity.access(http),
                HttpIdentity.cookie(http, HttpProtocol.REFRESH_COOKIE),
                http.getHeader(HttpProtocol.CSRF_HEADER),
                http.getRemoteAddr());
    }

    /**
     * 提取过滤器使用的页面SID和访问令牌。
     *
     * @param http 本次转换的http快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthCommand check(HttpServletRequest http) {
        return check(
                http.getHeader(HttpProtocol.SESSION_HEADER),
                HttpIdentity.access(http),
                http.getRemoteAddr());
    }

    /**
     * 为同步订阅构造只读鉴权命令，不继承共享刷新凭据。
     *
     * @param sid 本次转换的sid快照
     * @param access 本次转换的access快照
     * @param rateKey 本次转换的rateKey快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthCommand check(String sid, String access, String rateKey) {
        return new AuthCommand(
                "CHECK", null, null, null, null, null, null, sid, access, null, null, rateKey);
    }

    /**
     * 投影公开认证结果，不暴露刷新令牌或内部主键。
     *
     * @param result 本次转换的result快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthResponse toResponse(AuthResult result) {
        return new AuthResponse(
                result.userPublicId(),
                result.email(),
                result.sid(),
                result.accessToken(),
                result.csrf(),
                result.challengeId());
    }

    /**
     * 解释允许的认证路由。
     *
     * @param action HTTP动作名称
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String action(String action) {
        return switch (action) {
            case "challenge" -> "CHALLENGE";
            case "register" -> "REGISTER";
            case "login" -> "LOGIN";
            case "refresh" -> "REFRESH";
            case "reset" -> "RESET";
            case "logout" -> "LOGOUT";
            default -> throw new AdaptorException(AdaptorErrorCode.NOT_FOUND);
        };
    }
}
