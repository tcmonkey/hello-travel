package com.hellotravel.domain.persistence.model.param;

/**
 * MemorySummary完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MemorySummaryWriteParam(
        com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate aggregate) {
        }
