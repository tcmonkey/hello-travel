package com.hellotravel.adaptor.http.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.common.error.BaseException;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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

    public SessionFilter(
            AuthApplication application, ObjectMapper mapper, Environment environment) {
        this.application = application;
        this.mapper = mapper;
        this.environment = environment;
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
        String trace = Ids.next();
        long started = System.nanoTime();
        MDC.put("traceId", trace);
        response.setHeader("X-Trace-ID", trace);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Cache-Control", "no-store");
        try {
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
                        throw new DomainException(DomainErrorCode.UNAUTHORIZED);
                    }
                }
                String path = request.getRequestURI();
                boolean open =
                        List.of("challenge", "register", "login", "refresh", "reset").stream()
                                .anyMatch(action -> path.equals("/api/v1/auth/" + action));
                if (!open) {
                    var data =
                            com.hellotravel.adaptor.http.support.HttpResults.required(
                                    application.authenticate(
                                            new AuthCommand(
                                                    "CHECK",
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    null,
                                                    request.getHeader("X-Session-ID"),
                                                    HttpIdentity.access(request),
                                                    null,
                                                    null,
                                                    request.getRemoteAddr())));
                    request.setAttribute("ht.user", data.userId());
                    request.setAttribute("ht.session", data.sessionId());
                }
            }
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
