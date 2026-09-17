package com.hellotravel.domain.auth.model.aggregate;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record RefreshReceiptAggregate(
        com.hellotravel.domain.auth.model.entity.RefreshReceiptEntity entity) {
        }
