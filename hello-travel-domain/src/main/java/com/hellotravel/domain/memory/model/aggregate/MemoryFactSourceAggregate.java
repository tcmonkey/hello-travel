package com.hellotravel.domain.memory.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MemoryFactSourceAggregate(
        com.hellotravel.domain.memory.model.entity.MemoryFactSourceEntity entity) {
        }
