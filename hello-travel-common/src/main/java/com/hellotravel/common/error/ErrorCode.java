package com.hellotravel.common.error;

/**
 * 对外稳定错误分类契约。
 *
 * @author AIGenerator
 */
public interface ErrorCode {

    /**
     * 返回稳定错误标识。
     *
     * @return 标识
     * @author AIGenerator
     */
    String code();

    /**
     * 返回安全提示。
     *
     * @return 提示
     * @author AIGenerator
     */
    String message();

    /**
     * 返回HTTP状态语义。
     *
     * @return 状态
     * @author AIGenerator
     */
    int status();
}
