package com.hellotravel.domain.persistence.model.param;

/**
 * MemoryFactSource完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MemoryFactSourceWriteParam(
        com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate aggregate) {
        }
