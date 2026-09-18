package com.hellotravel.domain.persistence.model.param;

import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;

/**
 * OutboxEvent完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record OutboxEventWriteParam(OutboxEventAggregate aggregate) {
}
