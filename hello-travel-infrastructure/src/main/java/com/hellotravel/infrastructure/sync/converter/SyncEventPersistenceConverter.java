package com.hellotravel.infrastructure.sync.converter;

import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;
import com.hellotravel.infrastructure.sync.mysql.pojo.SyncEventPO;

import org.springframework.stereotype.Component;

/**
 * 持久化PO与完整领域快照双向映射；不执行IO或修改业务状态。
 *
 * @author AIGenerator
 */
@Component
public final class SyncEventPersistenceConverter {

    /**
     * 从完整数据库快照恢复领域聚合，不补默认业务状态。
     *
     * @param po 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public SyncEventAggregate restore(SyncEventPO po) {
        // 1. 逐字段恢复持久化事实，领域初始化默认值不覆盖数据库事实。
        return new SyncEventAggregate(
                new SyncEventEntity(
                        po.getId(),
                        po.getUserId(),
                        po.getEventSeq(),
                        po.getEventType(),
                        po.getAggregatePublicId(),
                        po.getAggregateVersion(),
                        po.getTargetSessionPublicId(),
                        po.getPayloadJson(),
                        po.getCreatedAt(),
                        po.getExpiresAt()));
    }

    /**
     * 整体映射聚合为数据库快照，版本CAS由仓储承担。
     *
     * @param aggregate 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public
    /**
     * 将完整聚合快照转换为本仓储PO，映射不参与业务状态决策。
     *
     * @param aggregate 待保存聚合
     * @return 数据库存储快照
     * @author AIGenerator
     */
    SyncEventPO toPersistence(SyncEventAggregate aggregate) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        SyncEventPO po = new SyncEventPO();
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
        po.setId(aggregate.entity().id());
        po.setUserId(aggregate.entity().userId());
        po.setEventSeq(aggregate.entity().eventSeq());
        po.setEventType(aggregate.entity().eventType());
        po.setAggregatePublicId(aggregate.entity().aggregatePublicId());
        po.setAggregateVersion(aggregate.entity().aggregateVersion());
        po.setTargetSessionPublicId(aggregate.entity().targetSessionPublicId());
        po.setPayloadJson(aggregate.entity().payloadJson());
        po.setCreatedAt(aggregate.entity().createdAt());
        po.setExpiresAt(aggregate.entity().expiresAt());
        // 3. 返回完整存储快照，由保存步骤决定新增或版本CAS更新。
        return po;
    }
}
