package com.hellotravel.adaptor.web.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.auth.input.assembler.AuthInputAssembler;
import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.common.error.BaseException;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
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

    private final AuthApplication application;
    private final ObjectMapper mapper;
    private final Environment environment;
    private final AuthInputAssembler authInputAssembler;

    public SessionFilter(
            AuthApplication application,
            ObjectMapper mapper,
            Environment environment,
            AuthInputAssembler authInputAssembler) {
        this.application = application;
        this.mapper = mapper;
        this.environment = environment;
        this.authInputAssembler = authInputAssembler;
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
                    var allowed =
                            List.of(
                                    environment
                                            .getProperty(
                                                    "ALLOWED_ORIGINS",
                                                    "http://localhost:5173,http://127.0.0.1:5173,http://loca"
                                                        + "lhost:8080,http://127.0.0.1:8080")
                                            .split(","));
                    if (origin == null || !allowed.contains(origin)) {
                        throw new AdaptorException(AdaptorErrorCode.UNAUTHORIZED);
                    }
                }
                String path = request.getRequestURI();
                boolean open =
                        List.of("challenge", "register", "login", "refresh", "reset").stream()
                                .anyMatch(action -> path.equals("/api/v1/auth/" + action));
                if (!open) {
                    var command = authInputAssembler.check(request);
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
