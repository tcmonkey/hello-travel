package com.hellotravel.domain.memory.model.param;

import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;

/**
 * MemoryFact完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MemoryFactWriteParam(MemoryFactAggregate aggregate) {
}
