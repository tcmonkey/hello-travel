package com.hellotravel.common.result;

import com.hellotravel.common.error.ErrorCode;

/**
 * 统一业务结果；失败不等于事务已经回滚。
 *
 * @param success 是否成功
 * @param code 稳定错误标识
 * @param message 安全消息
 * @param data 业务载荷
 * @param <T> 载荷类型
 * @author AIGenerator
 */
public record Result<T>(boolean success, String code, String message, T data) {

    /**
     * 构造成功业务结果。
     *
     * @author AIGenerator
     * @param data 受控业务载荷
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "OK", "成功", data);
    }

    /**
     * 构造仅包含安全分类的失败结果。
     *
     * @author AIGenerator
     * @param code 安全错误分类或验证码值，禁止日志输出
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public static <T> Result<T> failure(ErrorCode code) {
        return new Result<>(false, code.code(), code.message(), null);
    }

    /**
     * 拒绝失败结果，交由事务边界回滚。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public T require() {
        // 1. 依据下层标准结果的成功状态处理分支，避免继续使用无效数据。
        if (!success) {
            throw new IllegalStateException(code);
        }
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return data;
    }
}
