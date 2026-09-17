package com.hellotravel.adaptor.http.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从认证过滤器获取归属，忽略客户端提交的用户和登录主键。
 *
 * @author AIGenerator
 */
public final class HttpIdentity {

    /**
     * 建立HttpIdentity并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private HttpIdentity() {
    }

    /**
     * 读取认证过滤器提供的账号主键。
     *
     * @author AIGenerator
     * @param request HTTP请求及认证上下文
     * @return 归属和状态校验后的业务快照
     */
    public static Long user(HttpServletRequest request) {
        return (Long) request.getAttribute("ht.user");
    }

    /**
     * 读取原页面登录内部主键。
     *
     * @author AIGenerator
     * @param request HTTP请求及认证上下文
     * @return 归属和状态校验后的业务快照
     */
    public static Long session(HttpServletRequest request) {
        return (Long) request.getAttribute("ht.session");
    }

    /**
     * 提取页面访问凭据，不输出日志。
     *
     * @author AIGenerator
     * @param request HTTP请求及认证上下文
     * @return 当前操作的业务结果
     */
    public static String access(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    /**
     * 按名称读取请求Cookie。
     *
     * @author AIGenerator
     * @param request HTTP请求及认证上下文
     * @param name 受控name参数
     * @return 当前操作的业务结果
     */
    public static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
