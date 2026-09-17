package com.hellotravel.domain.persistence.model.param;

/**
 * MemoryFact完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MemoryFactWriteParam(
        com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate aggregate) {
        }
