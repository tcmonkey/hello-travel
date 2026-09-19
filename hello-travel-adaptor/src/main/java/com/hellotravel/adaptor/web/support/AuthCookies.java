package com.hellotravel.adaptor.web.support;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.application.auth.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.auth.security.assembler.SecurityCommandAssembler;
import com.hellotravel.common.identity.Ids;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 设备与刷新Cookie生命周期；与认证用例映射分离。
 *
 * @author AIGenerator
 */
@Component
public final class AuthCookies {

    private final SecurityOutAdaptor security;
    private final Environment environment;
    private final SecurityCommandAssembler securityCommandAssembler;

    public AuthCookies(
            SecurityOutAdaptor security,
            Environment environment,
            SecurityCommandAssembler securityCommandAssembler) {
        this.security = security;
        this.environment = environment;
        this.securityCommandAssembler = securityCommandAssembler;
    }

    /**
     * 验证或签发稳定设备Cookie。
     *
     * @param httpServletRequest 当前HTTP请求
     * @param response 当前HTTP响应
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String device(HttpServletRequest httpServletRequest, HttpServletResponse response) {
        // 1. 读取设备签名Cookie，格式合法时进行常量时间签名校验。
        String cookie = HttpIdentity.cookie(httpServletRequest, HttpProtocol.DEVICE_COOKIE);
        // 2. 仅接受有界设备键和签名，拒绝客户端伪造设备身份。
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
        // 3. 无合法设备证明时签发新随机设备键。
        String key = Ids.token();
        // 4. 将设备键与服务器签名一起写入受保护Cookie。
        cookie(
                response,
                HttpProtocol.DEVICE_COOKIE,
                key + "." + sign(key),
                HttpProtocol.DEVICE_COOKIE_SECONDS);
        // 5. 返回本次认证绑定的设备键。
        return key;
    }

    /**
     * 通过安全端口取得设备签名。
     *
     * @param key 设备随机键
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    private String sign(String key) {
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var result = security.process(securityCommandAssembler.signDevice(key));
        // 2. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new AdaptorException(AdaptorErrorCode.UNAVAILABLE);
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return result.data().value();
    }

    /**
     * 写入同源且禁止脚本读取的Cookie。
     *
     * @param response HTTP响应
     * @param name Cookie名称
     * @param value Cookie值
     * @param seconds 有效期秒数
     * @author AIGenerator
     */
    private void cookie(HttpServletResponse response, String name, String value, long seconds) {
        // 1. 取得安全端口的证明结果，供本段后续处理使用。
        boolean secure = environment.getProperty("COOKIE_SECURE", Boolean.class, false);
        // 2. 执行addHeader职责步骤，并把失败交给所属事务或入口处理。
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

    /**
     * 轮换认证成功的刷新Cookie，退出时立即清除。
     *
     * @param response 本次转换的response快照
     * @param command 本次转换的command快照
     * @param result 本次转换的result快照
     * @author AIGenerator
     */
    public void writeSession(HttpServletResponse response, AuthCommand command, AuthResult result) {
        // 1. 已成功签发刷新凭据时按认证有效期更新Cookie。
        if (result.refreshToken() != null) {
            cookie(
                    response,
                    HttpProtocol.REFRESH_COOKIE,
                    result.refreshToken(),
                    HttpProtocol.REFRESH_COOKIE_SECONDS);
        }
        // 2. 已完成退出时清除共享Cookie，不改变其他设备会话。
        if ("LOGOUT".equals(command.action())) {
            cookie(response, HttpProtocol.REFRESH_COOKIE, "", 0);
        }
    }
}
