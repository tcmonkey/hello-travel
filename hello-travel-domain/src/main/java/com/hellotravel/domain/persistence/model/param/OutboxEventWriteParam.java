package com.hellotravel.domain.persistence.model.param;

/**
 * OutboxEvent完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record OutboxEventWriteParam(
        com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate aggregate) {
        }
