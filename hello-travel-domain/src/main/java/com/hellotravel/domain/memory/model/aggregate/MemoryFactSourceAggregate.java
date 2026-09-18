package com.hellotravel.domain.memory.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.entity.MemoryFactSourceEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MemoryFactSourceAggregate(MemoryFactSourceEntity entity) {

    /**
     * 校验聚合输入完整性，防止空实体进入业务保存。
     *
     * @author AIGenerator
     */
    public void assertComplete() {
        // 1. 拒绝空实体容器，完整聚合才可以进入保存流程。
        if (entity == null) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 提供仓储加载所需的标识；标量状态仍只由实体持有。
     *
     * @return 已保存标识或新增时空值
     * @author AIGenerator
     */
    public Long idForPersistence() {
        return entity.id();
    }

    /**
     * 委托实体核对已持久化聚合的更新约束，不在领域服务展开实体属性。
     *
     * @param stored 已恢复的聚合
     * @author AIGenerator
     */
    public void assertWritableAgainst(MemoryFactSourceAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 记录长期事实的原始用户消息与版本证据；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param conversationId 一期禁止跨对话共享长期事实
     * @param factId 长期事实标识
     * @param messageId 来源原始用户消息标识
     * @param evidenceExcerpt 可核对的有界证据摘录，来源删除后同步清除
     * @param messageVersion 引用时原消息版本
     * @param createdAt 创建时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MemoryFactSourceAggregate evidence(
            Long userId,
            Long conversationId,
            Long factId,
            Long messageId,
            String evidenceExcerpt,
            Long messageVersion,
            java.time.LocalDateTime createdAt) {
        // 1. 记录长期事实的原始用户消息与版本证据，具体变更委托实体。
        return new MemoryFactSourceAggregate(
                MemoryFactSourceEntity.evidence(
                        userId,
                        conversationId,
                        factId,
                        messageId,
                        evidenceExcerpt,
                        messageVersion,
                        createdAt));
    }
}
