package com.hellotravel.adaptor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.auth.input.assembler.AuthAssembler;
import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.common.error.BaseException;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 同源写入和页面SID鉴权；错误在过滤器边界转换为安全HTTP结果。
 *
 * @author AIGenerator
 */
@Component
@Order(10)
public final class SessionFilter extends OncePerRequestFilter {

    /**
     * 认证业务动作入口。
     *
     * @author AIGenerator
     */
    private final AuthAppService application;
    /**
     * 过滤器失败响应的 JSON 序列化器。
     *
     * @author AIGenerator
     */
    private final ObjectMapper mapper;
    /**
     * 允许发起受保护写请求的浏览器源列表。
     *
     * @author AIGenerator
     */
    private final List<String> allowedOrigins;
    /**
     * HTTP 请求到认证命令的组装器。
     *
     * @author AIGenerator
     */
    private final AuthAssembler authAssembler;

    /**
     * 创建会话过滤器并装配固定依赖。
     *
     * @author AIGenerator
     * @param application 认证业务动作入口
     * @param mapper 失败响应序列化器
     * @param allowedOrigins 允许的浏览器源配置
     * @param authAssembler 认证命令组装器
     */
    public SessionFilter(
            AuthAppService application,
            ObjectMapper mapper,
            @Value("\u0024{travel.security.allowed-origins}") String allowedOrigins,
            AuthAssembler authAssembler) {
        this.application = application;
        this.mapper = mapper;
        this.allowedOrigins = List.of(allowedOrigins.split(","));
        this.authAssembler = authAssembler;
    }

    /**
     * 处理doFilterInternal对应的受控业务操作。
     *
     * @author AIGenerator
     * @param request HTTP请求及认证上下文
     * @param response HTTP响应
     * @param chain 后续HTTP过滤器链
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 1. 取得关联当前请求的追踪标识，供本段后续处理使用。
        String trace = Ids.next();
        long started = System.nanoTime();
        // 2. 执行put职责步骤，并把失败交给所属事务或入口处理。
        MDC.put("traceId", trace);
        // 3. 映射本段快照字段，业务状态规则不放入PO赋值。
        response.setHeader("X-Trace-ID", trace);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Cache-Control", "no-store");
        // 4. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try {
            // 1. 仅保护本服务API路径，请求身份校验不干扰静态资源。
            if (request.getRequestURI().startsWith("/api/v1/")) {
                if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
                    String origin = request.getHeader("Origin");
                    if (origin == null || !allowedOrigins.contains(origin)) {
                        throw new AdaptorException(AdaptorErrorCode.UNAUTHORIZED);
                    }
                }
                String path = request.getRequestURI();
                boolean open =
                        List.of("challenge", "login", "refresh", "reset").stream()
                                .anyMatch(action -> path.equals("/api/v1/auth/" + action))
                                || ("GET".equals(request.getMethod())
                                        && path.matches(
                                                "/api/v1/auth/challenge/[0-9A-HJKMNP-TV-Z]{26}/status"));
                if (!open) {
                    var command = authAssembler.check(request);
                    var result = application.authenticate(command);
                    var data = HttpResults.required(result);
                    request.setAttribute("ht.user", data.userId());
                    request.setAttribute("ht.session", data.sessionId());
                }
            }
            // 2. 执行doFilter职责步骤，并把失败交给所属事务或入口处理。
            chain.doFilter(request, response);
        } catch (BaseException exception) {
            if (!response.isCommitted()) {
                response.setStatus(exception.errorCode().status());
                response.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(
                        response.getOutputStream(), Result.failure(exception.errorCode()));
            }
        } finally {
            MDC.remove("traceId");
        }
    }
}
