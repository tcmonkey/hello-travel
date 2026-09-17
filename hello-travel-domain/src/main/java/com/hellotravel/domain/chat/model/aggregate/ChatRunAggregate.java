package com.hellotravel.domain.chat.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record ChatRunAggregate(com.hellotravel.domain.chat.model.entity.ChatRunEntity entity) {
}
