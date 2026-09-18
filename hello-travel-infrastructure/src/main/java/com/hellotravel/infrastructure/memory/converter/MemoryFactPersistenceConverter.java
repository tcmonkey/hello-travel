package com.hellotravel.infrastructure.memory.converter;

import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.entity.MemoryFactEntity;
import com.hellotravel.infrastructure.memory.mysql.pojo.MemoryFactPO;

import org.springframework.stereotype.Component;

/**
 * 持久化PO与完整领域快照双向映射；不执行IO或修改业务状态。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryFactPersistenceConverter {

    /**
     * 从完整数据库快照恢复领域聚合，不补默认业务状态。
     *
     * @param po 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public MemoryFactAggregate restore(MemoryFactPO po) {
        // 1. 逐字段恢复持久化事实，领域初始化默认值不覆盖数据库事实。
        return new MemoryFactAggregate(
                new MemoryFactEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getMemoryEpoch(),
                        po.getFactKey(),
                        po.getCategory(),
                        po.getContent(),
                        po.getEvidenceType(),
                        po.getSourceCount(),
                        po.getStatus(),
                        po.getExpiresAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
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
    MemoryFactPO toPersistence(MemoryFactAggregate aggregate) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        MemoryFactPO po = new MemoryFactPO();
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
        po.setId(aggregate.entity().id());
        po.setPublicId(aggregate.entity().publicId());
        po.setUserId(aggregate.entity().userId());
        po.setConversationId(aggregate.entity().conversationId());
        po.setMemoryEpoch(aggregate.entity().memoryEpoch());
        po.setFactKey(aggregate.entity().factKey());
        po.setCategory(aggregate.entity().category());
        po.setContent(aggregate.entity().content());
        po.setEvidenceType(aggregate.entity().evidenceType());
        po.setSourceCount(aggregate.entity().sourceCount());
        po.setStatus(aggregate.entity().status());
        po.setExpiresAt(aggregate.entity().expiresAt());
        po.setCreatedAt(aggregate.entity().createdAt());
        po.setUpdatedAt(aggregate.entity().updatedAt());
        po.setVersion(aggregate.entity().version());
        // 3. 返回完整存储快照，由保存步骤决定新增或版本CAS更新。
        return po;
    }
}
