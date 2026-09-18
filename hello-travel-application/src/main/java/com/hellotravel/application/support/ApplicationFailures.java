package com.hellotravel.application.support;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;

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
        // 1. 连接类故障映射为暂不可用，调用者可以按协议重试。
        if (exception instanceof DataAccessResourceFailureException) {
            return Result.failure(ApplicationErrorCode.UNAVAILABLE);
        }
        // 2. 唯一约束或暂态写冲突映射为CONFLICT，不吞掉事务失败。
        if (exception instanceof DataIntegrityViolationException
                || exception instanceof TransientDataAccessException) {
            return Result.failure(ApplicationErrorCode.CONFLICT);
        }
        // 3. 非法输入映射为稳定参数错误，技术消息不向外泄漏。
        if (exception instanceof IllegalArgumentException) {
            return Result.failure(ApplicationErrorCode.INVALID);
        }
        // 4. 返回本段实际处理结果，保持本层输出契约。
        return Failures.capture(exception, ApplicationErrorCode.FAILED);
    }

    /**
     * 事务内部验证下层结果，失败中断执行，回滚后由应用入口统一捕获。
     *
     * @param result 下层已分类结果
     * @param <T> 下层成功载荷类型
     * @return 确认有效的成功载荷
     * @author AIGenerator
     */
    public static <T> T required(Result<T> result) {
        // 1. 下层缺少标准结果时拒绝继续执行，避免把空结果当作成功。
        if (result == null) {
            throw new ApplicationException(ApplicationErrorCode.FAILED);
        }
        // 2. 保留当前稳定分类并中断事务，不能让失败Result继续提交。
        if (!result.success()) {
            throw new ApplicationException(ApplicationErrorCode.valueOf(result.code()));
        }
        // 3. 成功载荷缺失同样中断，成功标志不能代替真实持久化结果。
        if (result.data() == null) {
            throw new ApplicationException(ApplicationErrorCode.FAILED);
        }
        // 4. 返回确认有效的业务载荷，工具不处理领域路由或仓储操作。
        return result.data();
    }
}
