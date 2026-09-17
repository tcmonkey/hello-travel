package com.hellotravel.domain.knowledge.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param documentId 目标文档
 * @param indexGeneration 任务绑定的索引代次
 * @param jobType 导入或删除索引任务
 * @param status 任务状态
 * @param attemptCount 当前尝试次数
 * @param maxAttempts 重试次数上限
 * @param nextAttemptAt 下次可执行时间
 * @param leaseOwner 任务执行者
 * @param leaseFence 执行租约代次
 * @param leaseUntil 租约截止时间
 * @param errorCode 稳定错误码
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record IndexJobEntity(
        Long id,
        String publicId,
        Long userId,
        Long documentId,
        Long indexGeneration,
        String jobType,
        String status,
        Integer attemptCount,
        Integer maxAttempts,
        java.time.LocalDateTime nextAttemptAt,
        String leaseOwner,
        Long leaseFence,
        java.time.LocalDateTime leaseUntil,
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
        return "IndexJobEntity{redacted}";
    }
}
