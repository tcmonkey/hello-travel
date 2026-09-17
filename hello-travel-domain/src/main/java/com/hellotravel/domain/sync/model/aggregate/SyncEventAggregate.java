package com.hellotravel.domain.sync.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record SyncEventAggregate(com.hellotravel.domain.sync.model.entity.SyncEventEntity entity) {
}
