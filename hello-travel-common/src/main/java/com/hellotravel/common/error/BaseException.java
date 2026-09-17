package com.hellotravel.common.error;

/**
 * 保留内部错误分类，HTTP边界只输出安全消息。
 *
 * @author AIGenerator
 */
public class BaseException extends RuntimeException {

    /**
     * 错误分类。
     *
     * @author AIGenerator
     */
    private final ErrorCode errorCode;

    /**
     * 建立BaseException并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param errorCode 受控errorCode参数
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 处理errorCode对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
