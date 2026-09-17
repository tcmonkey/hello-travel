package com.hellotravel.infrastructure.exception;

import com.hellotravel.common.error.BaseException;

/**
 * Infrastructure层可安全跨边界的失败。
 *
 * @author AIGenerator
 */
public final class InfrastructureException extends BaseException {

    /**
     * 建立InfrastructureException并保存明确业务依赖。
     *
     * @author AIGenerator
     * @param errorCode 受控errorCode参数
     */
    public InfrastructureException(InfrastructureErrorCode errorCode) {
        super(errorCode);
    }
}
