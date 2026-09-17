package com.hellotravel.domain.knowledge.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record KnowledgeChunkAggregate(
        com.hellotravel.domain.knowledge.model.entity.KnowledgeChunkEntity entity) {
        }
