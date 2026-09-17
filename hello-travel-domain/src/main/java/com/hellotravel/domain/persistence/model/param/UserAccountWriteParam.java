package com.hellotravel.domain.persistence.model.param;

/**
 * UserAccount完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record UserAccountWriteParam(
        com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate aggregate) {
        }
