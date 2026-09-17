package com.hellotravel.adaptor.exception;

import com.hellotravel.common.error.BaseException;

/**
 * Adaptor层可安全跨边界的失败。
 *
 * @author AIGenerator
 */
public final class AdaptorException extends BaseException {

    /**
     * 建立AdaptorException并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param errorCode 受控errorCode参数
     */
    public AdaptorException(AdaptorErrorCode errorCode) {
        super(errorCode);
    }
}
