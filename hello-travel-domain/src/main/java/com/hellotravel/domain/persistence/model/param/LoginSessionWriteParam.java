package com.hellotravel.domain.persistence.model.param;

/**
 * LoginSession完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record LoginSessionWriteParam(
        com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate aggregate) {
        }
