package com.hellotravel.domain.sync.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 可选账号归属；注册验证码可尚无账号
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
}
