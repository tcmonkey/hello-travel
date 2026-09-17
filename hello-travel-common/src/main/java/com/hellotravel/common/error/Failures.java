package com.hellotravel.common.error;

import com.hellotravel.common.result.Result;

/**
 * 保留已标准化的错误分类，隔离未知异常中的技术详情。
 *
 * @author AIGenerator
 */
public final class Failures {

    /**
     * 建立Failures并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private Failures() {
    }

    /**
     * 将入口捕获的异常转换为安全失败结果，不返回异常正文或堆栈。
     *
     * @param exception 入口捕获的异常
     * @param fallback 未预期异常的本层分类
     * @param <T> 成功载荷类型
     * @return 失败结果，保留业务异常的稳定分类
     * @author AIGenerator
     */
    public static <T> Result<T> capture(Exception exception, ErrorCode fallback) {
        if (exception instanceof BaseException business) {
            return Result.failure(business.errorCode());
        }
        return Result.failure(fallback);
    }
}
