package com.hellotravel.domain.persistence.model.param;

import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;

/**
 * KnowledgeChunk完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record KnowledgeChunkWriteParam(KnowledgeChunkAggregate aggregate) {
}
