package com.hellotravel.adaptor.common;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.common.error.BaseException;
import com.hellotravel.common.result.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 业务异常保留状态分类，验证与持久化异常不暴露SQL、认证信息或调用正文。
 *
 * @author AIGenerator
 */
@RestControllerAdvice
public final class HttpErrors {

    /**
     * 仅记录分类和trace的安全故障日志。
     *
     * @author AIGenerator
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpErrors.class);

    /**
     * 映射业务失败为对应HTTP状态。
     *
     * @author AIGenerator
     * @param exception 待分类的失败
     * @return 当前操作的业务结果
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> business(BaseException exception) {
        return ResponseEntity.status(exception.errorCode().status())
                .body(Result.failure(exception.errorCode()));
    }

    /**
     * 将无效协议输入转换为安全错误。
     *
     * @author AIGenerator
     * @param exception 待分类的失败
     * @return 当前操作的业务结果
     */
    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MaxUploadSizeExceededException.class
    })
    public ResponseEntity<Result<Void>> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(Result.failure(AdaptorErrorCode.INVALID));
    }

    /**
     * 将持久化唯一约束冲突转换为HTTP冲突。
     *
     * @author AIGenerator
     * @param exception 待分类的失败
     * @return 当前操作的业务结果
     */
    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Result<Void>> conflict(Exception exception) {
        return ResponseEntity.status(409).body(Result.failure(AdaptorErrorCode.CONFLICT));
    }

    /**
     * 记录安全故障分类并返回通用服务错误。
     *
     * @author AIGenerator
     * @param exception 待分类的失败
     * @return 当前操作的业务结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception exception) {
        // 1. 执行error职责步骤，并把失败交给所属事务或入口处理。
        LOGGER.error(
                "request_failed type={} traceId={}",
                exception.getClass().getSimpleName(),
                MDC.get("traceId"));
        // 2. 传播稳定失败分类，不泄漏原始技术异常。
        return ResponseEntity.status(500).body(Result.failure(AdaptorErrorCode.FAILED));
    }
}
