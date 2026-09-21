package com.hellotravel.domain.sync.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 可选账号归属；首次登录验证码可尚无账号
 * @param eventType 推送、开始生成、提取记忆或验证码投递等事件
 * @param dedupeKey 事件投递幂等键
 * @param payloadJson 只存受限任务引用，验证码密文在challenge表
 * @param status 派发状态
 * @param attemptCount 派发尝试次数
 * @param maxAttempts 派发次数上限，死信可诊断
 * @param nextAttemptAt 下次派发时间
 * @param leaseOwner 派发执行者
 * @param leaseFence 派发租约代次
 * @param leaseUntil 租约到期时间
 * @param deliveredAt 实际完成/幂等交付时间
 * @param errorCode 稳定内部错误码
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record OutboxEventEntity(
        Long id,
        String publicId,
        Long userId,
        String eventType,
        String dedupeKey,
        String payloadJson,
        String status,
        Integer attemptCount,
        Integer maxAttempts,
        java.time.LocalDateTime nextAttemptAt,
        String leaseOwner,
        Long leaseFence,
        java.time.LocalDateTime leaseUntil,
        java.time.LocalDateTime deliveredAt,
        String errorCode,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "OutboxEventEntity{redacted}";
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(OutboxEventEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 创建待执行任务并固定初始尝试与租约，固定状态由实体封装。
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
    public static OutboxEventEntity pending(
            Long userId,
            String eventType,
            String dedupeKey,
            String payloadJson,
            java.time.LocalDateTime nextAttemptAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待执行任务并固定初始尝试与租约，返回不可变快照。
        return new OutboxEventEntity(
                null,
                Ids.next(),
                userId,
                eventType,
                dedupeKey,
                payloadJson,
                "PENDING",
                0,
                8,
                nextAttemptAt,
                null,
                0L,
                null,
                null,
                null,
                createdAt,
                updatedAt,
                0L);
    }

    /**
     * 标记已领取任务并提高执行栅栏，固定状态由实体封装。
     *
     * @param attemptCount 显式重试代次，不重复创建用户消息
     * @param leaseFence 租约代次，旧执行者不可提交
     * @param leaseUntil 执行租约到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public OutboxEventEntity claimed(
            Integer attemptCount, Long leaseFence, java.time.LocalDateTime leaseUntil) {
        // 1. 标记已领取任务并提高执行栅栏，返回不可变快照。
        return new OutboxEventEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.eventType(),
                this.dedupeKey(),
                this.payloadJson(),
                "DELIVERING",
                attemptCount,
                this.maxAttempts(),
                this.nextAttemptAt(),
                Ids.next(),
                leaseFence,
                leaseUntil,
                this.deliveredAt(),
                this.errorCode(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 记录当前处理结果并保持原始归属与版本，固定状态由实体封装。
     *
     * @param status 有效、失效或到期
     * @param nextAttemptAt 下次可执行时间
     * @param deliveredAt 实际完成/幂等交付时间
     * @param errorCode 内部稳定错误码，不保存原始供应商错误
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public OutboxEventEntity completed(
            String status,
            java.time.LocalDateTime nextAttemptAt,
            java.time.LocalDateTime deliveredAt,
            String errorCode) {
        // 1. 记录当前处理结果并保持原始归属与版本，返回不可变快照。
        return new OutboxEventEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.eventType(),
                this.dedupeKey(),
                this.payloadJson(),
                status,
                this.attemptCount(),
                this.maxAttempts(),
                nextAttemptAt,
                this.leaseOwner(),
                this.leaseFence(),
                null,
                deliveredAt,
                errorCode,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }
}
