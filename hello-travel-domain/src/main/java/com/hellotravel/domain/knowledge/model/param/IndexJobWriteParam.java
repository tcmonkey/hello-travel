package com.hellotravel.domain.knowledge.model.param;

import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;

/**
 * IndexJob完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record IndexJobWriteParam(IndexJobAggregate aggregate) {
}
