package com.hellotravel.domain.knowledge.model.param;

import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;

/**
 * KnowledgeDocument完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record KnowledgeDocumentWriteParam(KnowledgeDocumentAggregate aggregate) {
}
