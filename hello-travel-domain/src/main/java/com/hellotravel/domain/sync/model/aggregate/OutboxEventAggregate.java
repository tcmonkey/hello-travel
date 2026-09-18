package com.hellotravel.domain.sync.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.sync.model.entity.OutboxEventEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record OutboxEventAggregate(OutboxEventEntity entity) {

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
    public void assertWritableAgainst(OutboxEventAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 创建待执行任务并固定初始尝试与租约；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param eventType 推送、开始生成、提取记忆或验证码投递等事件
     * @param dedupeKey 事件投递幂等键
     * @param payloadJson 只存受限任务引用，验证码密文在challenge表
     * @param nextAttemptAt 下次可执行时间
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static OutboxEventAggregate pending(
            Long userId,
            String eventType,
            String dedupeKey,
            String payloadJson,
            java.time.LocalDateTime nextAttemptAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待执行任务并固定初始尝试与租约，具体变更委托实体。
        return new OutboxEventAggregate(
                OutboxEventEntity.pending(
                        userId,
                        eventType,
                        dedupeKey,
                        payloadJson,
                        nextAttemptAt,
                        createdAt,
                        updatedAt));
    }

    /**
     * 标记已领取任务并提高执行栅栏；聚合委托实体，不重复存储标量状态。
     *
     * @param attemptCount 显式重试代次，不重复创建用户消息
     * @param leaseFence 租约代次，旧执行者不可提交
     * @param leaseUntil 执行租约到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public OutboxEventAggregate claimed(
            Integer attemptCount, Long leaseFence, java.time.LocalDateTime leaseUntil) {
        // 1. 标记已领取任务并提高执行栅栏，具体变更委托实体。
        return new OutboxEventAggregate(entity.claimed(attemptCount, leaseFence, leaseUntil));
    }

    /**
     * 记录当前处理结果并保持原始归属与版本；聚合委托实体，不重复存储标量状态。
     *
     * @param status 有效、失效或到期
     * @param nextAttemptAt 下次可执行时间
     * @param deliveredAt 实际完成/幂等交付时间
     * @param errorCode 内部稳定错误码，不保存原始供应商错误
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public OutboxEventAggregate completed(
            String status,
            java.time.LocalDateTime nextAttemptAt,
            java.time.LocalDateTime deliveredAt,
            String errorCode) {
        // 1. 记录当前处理结果并保持原始归属与版本，具体变更委托实体。
        return new OutboxEventAggregate(
                entity.completed(status, nextAttemptAt, deliveredAt, errorCode));
    }
}
