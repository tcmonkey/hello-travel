package com.hellotravel.application.support;

import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;

/**
 * 应用入口在事务完成或回滚后，将业务和持久化失败转换为稳定结果。
 *
 * @author AIGenerator
 */
public final class ApplicationFailures {

    /**
     * 建立ApplicationFailures并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private ApplicationFailures() {
    }

    /**
     * 分类入口异常；资源不可用、冲突和非法输入不混为未知故障。
     *
     * @param exception 完成事务回滚后捕获的失败
     * @param <T> 成功载荷类型
     * @return 应用失败结果
     * @author AIGenerator
     */
    public static <T> Result<T> capture(Exception exception) {
        if (exception instanceof DataAccessResourceFailureException) {
            return Result.failure(DomainErrorCode.UNAVAILABLE);
        }
        if (exception instanceof DataIntegrityViolationException
                || exception instanceof TransientDataAccessException) {
            return Result.failure(DomainErrorCode.CONFLICT);
        }
        if (exception instanceof IllegalArgumentException) {
            return Result.failure(DomainErrorCode.INVALID);
        }
        return Failures.capture(exception, DomainErrorCode.FAILED);
    }
}
