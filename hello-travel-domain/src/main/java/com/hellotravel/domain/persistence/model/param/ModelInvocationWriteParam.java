package com.hellotravel.domain.persistence.model.param;

/**
 * ModelInvocation完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record ModelInvocationWriteParam(
        com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate aggregate) {
        }
