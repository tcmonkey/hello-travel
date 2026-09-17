package com.hellotravel.adaptor.http.input;

import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.command.SecurityCommand;
import com.hellotravel.client.auth.request.AuthRequest;
import com.hellotravel.client.auth.response.AuthResponse;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 页面SID绑定协议；旧页面不得借共享Cookie继承新登录。
 *
 * @author AIGenerator
 */
@RestController
@RequestMapping("/api/v1/auth")
public final class AuthController {

    private final AuthApplication application;

    private final SecurityOutAdaptor security;

    private final Environment environment;

    public AuthController(
            AuthApplication application, SecurityOutAdaptor security, Environment environment) {
        this.application = application;
        this.security = security;
        this.environment = environment;
    }

    /**
     * 执行指定认证用例，失败异常越过事务边界。
     *
     * @author AIGenerator
     * @param action 受控action参数
     * @param authRequest 已验证的公开请求参数
     * @param httpServletRequest HTTP请求及认证上下文
     * @param response HTTP响应
     * @return 当前操作的业务结果
     */
    @PostMapping("/{action}")
    public Result<AuthResponse> authenticate(
            @PathVariable String action,
            @Valid @RequestBody AuthRequest authRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response) {
        try {
            String selected =
                    switch (action) {
                        case "challenge" -> "CHALLENGE";
                        case "register" -> "REGISTER";
                        case "login" -> "LOGIN";
                        case "refresh" -> "REFRESH";
                        case "reset" -> "RESET";
                        case "logout" -> "LOGOUT";
                        default -> throw new DomainException(DomainErrorCode.NOT_FOUND);
                    };
            String device = device(httpServletRequest, response);
            var result =
                    application.authenticate(
                            new AuthCommand(
                                    selected,
                                    authRequest.email(),
                                    authRequest.password(),
                                    authRequest.challengeId(),
                                    authRequest.code(),
                                    authRequest.purpose(),
                                    device,
                                    httpServletRequest.getHeader("X-Session-ID"),
                                    HttpIdentity.access(httpServletRequest),
                                    HttpIdentity.cookie(httpServletRequest, "ht_refresh"),
                                    httpServletRequest.getHeader("X-CSRF-Token"),
                                    httpServletRequest.getRemoteAddr()));
            if (!result.success()) {
                return com.hellotravel.adaptor.http.support.HttpResults.failure(result);
            }
            var data = result.data();
            if (data.refreshToken() != null) {
                cookie(response, "ht_refresh", data.refreshToken(), 604800);
            }
            if ("LOGOUT".equals(selected)) {
                cookie(response, "ht_refresh", "", 0);
            }
            return Result.success(
                    new AuthResponse(
                            data.userPublicId(),
                            data.email(),
                            data.sid(),
                            data.accessToken(),
                            data.csrf(),
                            data.challengeId()));
        } catch (Exception exception) {
            return com.hellotravel.adaptor.http.support.HttpResults.capture(exception);
        }
    }

    private String device(HttpServletRequest httpServletRequest, HttpServletResponse response) {
        String cookie = HttpIdentity.cookie(httpServletRequest, "ht_device");
        if (cookie != null && cookie.length() < 200) {
            String[] parts = cookie.split("\\.");
            if (parts.length == 2 && parts[0].matches("[a-zA-Z0-9_-]{43}")) {
                String signature = sign(parts[0]);
                if (java.security.MessageDigest.isEqual(
                        signature.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        parts[1].getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    return parts[0];
                }
            }
        }
        String key = Ids.token();
        cookie(response, "ht_device", key + "." + sign(key), 31536000);
        return key;
    }

    private String sign(String key) {
        var result = security.process(new SecurityCommand("SIGN_DEVICE", key, null, "device-v1"));
        if (!result.success()) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
        return result.data().value();
    }

    private void cookie(HttpServletResponse response, String name, String value, long seconds) {
        boolean secure = environment.getProperty("COOKIE_SECURE", Boolean.class, false);
        response.addHeader(
                "Set-Cookie",
                ResponseCookie.from(name, value)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Strict")
                        .path("/api/v1")
                        .maxAge(seconds)
                        .build()
                        .toString());
    }
}
