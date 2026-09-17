package com.hellotravel.domain.memory.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MemoryFactAggregate(
        com.hellotravel.domain.memory.model.entity.MemoryFactEntity entity) {
        }
