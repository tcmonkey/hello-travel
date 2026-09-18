package com.hellotravel.application.exception;

import com.hellotravel.common.error.BaseException;

/**
 * Application层可安全跨边界的失败。
 *
 * @author AIGenerator
 */
public final class ApplicationException extends BaseException {

    /**
     * 建立ApplicationException并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param errorCode 受控errorCode参数
     */
    public ApplicationException(ApplicationErrorCode errorCode) {
        super(errorCode);
    }
}
