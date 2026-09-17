package com.hellotravel.adaptor.http.support;

import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTP结果状态转换；协议入口必须自行捕获异常，不依赖全局处理器代替。
 *
 * @author AIGenerator
 */
public final class HttpResults {

    /**
     * 建立HttpResults并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private HttpResults() {
    }

    /**
     * 捕获异常后构建安全失败响应，同时设置对应HTTP失败状态。
     *
     * @param exception 协议入口捕获的失败
     * @param <T> 响应载荷类型
     * @return 统一失败响应
     * @author AIGenerator
     */
    public static <T> Result<T> capture(Exception exception) {
        return failure(ApplicationFailures.capture(exception));
    }

    /**
     * 向外传递安全失败分类，避免把失败转换为成功或丢失状态码。
     *
     * @param result 应用失败结果
     * @param <T> 对外载荷类型
     * @return 不含内部载荷的失败结果
     * @author AIGenerator
     */
    public static <T> Result<T> failure(Result<?> result) {
        DomainErrorCode code = classification(result.code());
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            var response = attributes.getResponse();
            if (response != null && !response.isCommitted()) {
                response.setStatus(code.status());
            }
        }
        return new Result<>(false, result.code(), result.message(), null);
    }

    /**
     * 内部辅助链路拒绝失败结果，供过滤器及事务之外的流订阅检查使用。
     *
     * @param result 已标准化业务结果
     * @param <T> 成功载荷类型
     * @return 成功载荷，失败由调用入口捕获
     * @author AIGenerator
     */
    public static <T> T required(Result<T> result) {
        if (!result.success()) {
            throw new DomainException(classification(result.code()));
        }
        return result.data();
    }

    private static DomainErrorCode classification(String code) {
        try {
            return DomainErrorCode.valueOf(code);
        } catch (IllegalArgumentException exception) {
            return DomainErrorCode.FAILED;
        }
    }
}
