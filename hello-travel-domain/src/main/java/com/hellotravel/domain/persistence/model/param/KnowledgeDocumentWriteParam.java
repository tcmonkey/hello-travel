package com.hellotravel.domain.persistence.model.param;

/**
 * KnowledgeDocument完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record KnowledgeDocumentWriteParam(
        com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate aggregate) {
        }
