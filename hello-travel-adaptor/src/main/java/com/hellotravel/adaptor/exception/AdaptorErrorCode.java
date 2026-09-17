package com.hellotravel.adaptor.exception;

import com.hellotravel.common.error.ErrorCode;

/**
 * Adaptor层稳定错误分类。
 *
 * @author AIGenerator
 */
public enum AdaptorErrorCode implements ErrorCode {

    /**
     * 定义INVALID错误分类。
     *
     * @author AIGenerator
     */
    INVALID(400, "请求参数无效"),
    /**
     * 定义UNAUTHORIZED错误分类。
     *
     * @author AIGenerator
     */
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    /**
     * 定义SESSION_REPLACED错误分类。
     *
     * @author AIGenerator
     */
    SESSION_REPLACED(401, "此页面的登录已被新登录替换"),
    /**
     * 定义NOT_FOUND错误分类。
     *
     * @author AIGenerator
     */
    NOT_FOUND(404, "记录不存在或无访问权限"),
    /**
     * 定义CONFLICT错误分类。
     *
     * @author AIGenerator
     */
    CONFLICT(409, "数据已变化，请刷新后重试"),
    /**
     * 定义BUSY错误分类。
     *
     * @author AIGenerator
     */
    BUSY(409, "请先停止当前生成"),
    /**
     * 定义IDEMPOTENCY_CONFLICT错误分类。
     *
     * @author AIGenerator
     */
    IDEMPOTENCY_CONFLICT(409, "重复请求键对应的内容不同"),
    /**
     * 定义SYNC_RESET_REQUIRED错误分类。
     *
     * @author AIGenerator
     */
    SYNC_RESET_REQUIRED(409, "同步游标已过期，需要重新恢复"),
    /**
     * 定义RATE_LIMITED错误分类。
     *
     * @author AIGenerator
     */
    RATE_LIMITED(429, "操作过于频繁，请稍后重试"),
    /**
     * 定义UNAVAILABLE错误分类。
     *
     * @author AIGenerator
     */
    UNAVAILABLE(503, "服务暂不可用，请稍后重试"),
    /**
     * 定义CONTEXT_LIMIT错误分类。
     *
     * @author AIGenerator
     */
    CONTEXT_LIMIT(422, "上下文超出可用预算"),
    /**
     * 定义FAILED错误分类。
     *
     * @author AIGenerator
     */
    FAILED(500, "操作未完成，请稍后重试");

    /**
     * 保存status对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final int status;

    /**
     * 保存message对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final String message;

    /**
     * 建立AdaptorErrorCode并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param status 受控status参数
     * @param message 受控message参数
     */
    AdaptorErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 返回稳定错误标识。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public String code() {
        return name();
    }

    /**
     * 返回可公开的安全提示。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public String message() {
        return message;
    }

    /**
     * 返回HTTP状态语义。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public int status() {
        return status;
    }
}
