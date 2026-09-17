package com.hellotravel.domain.persistence.model.param;

/**
 * IndexJob完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record IndexJobWriteParam(
        com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate aggregate) {
        }
