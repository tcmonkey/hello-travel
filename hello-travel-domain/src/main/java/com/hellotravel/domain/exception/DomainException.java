package com.hellotravel.domain.exception;

import com.hellotravel.common.error.BaseException;

/**
 * Domain层可安全跨边界的失败。
 *
 * @author AIGenerator
 */
public final class DomainException extends BaseException {

    /**
     * 建立DomainException并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param errorCode 受控errorCode参数
     */
    public DomainException(DomainErrorCode errorCode) {
        super(errorCode);
    }
}
