package com.hellotravel.domain.knowledge.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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

    /**
     * 领取待执行索引任务并提高租约栅栏，原始版本留给仓储CAS。
     *
     * @param owner 当前执行者
     * @param time 当前UTC时间
     * @return 已领取的任务快照
     * @author AIGenerator
     */
    public IndexJobEntity claim(String owner, java.time.LocalDateTime time) {
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("job not pending");
        }
        // 2. 领取待执行任务并提高租约栅栏，返回不可变快照。
        return new IndexJobEntity(
                id,
                publicId,
                userId,
                documentId,
                indexGeneration,
                jobType,
                "RUNNING",
                attemptCount + 1,
                maxAttempts,
                nextAttemptAt,
                owner,
                leaseFence + 1,
                time.plusMinutes(15),
                errorCode,
                createdAt,
                updatedAt,
                version);
    }

    /**
     * 结束当前租约尝试；是否允许提交由调用方的栅栏检查决定。
     *
     * @param success 本次是否成功
     * @param error 稳定错误分类
     * @return 终态任务快照
     * @author AIGenerator
     */
    public IndexJobEntity complete(boolean success, String error) {
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"RUNNING".equals(status)) {
            throw new IllegalStateException("job not running");
        }
        // 2. 完成本次尝试并释放活动租约，返回不可变快照。
        return new IndexJobEntity(
                id,
                publicId,
                userId,
                documentId,
                indexGeneration,
                jobType,
                success ? "SUCCEEDED" : "FAILED",
                attemptCount,
                maxAttempts,
                nextAttemptAt,
                leaseOwner,
                leaseFence,
                null,
                error,
                createdAt,
                updatedAt,
                version);
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(IndexJobEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对知识文档归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.documentId(), this.documentId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 创建待执行任务并固定初始尝试与租约，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param documentId 知识文档归属
     * @param indexGeneration 重试/重建的索引代次，防旧任务覆盖
     * @param jobType 导入或删除索引任务
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static IndexJobEntity pending(
            Long userId,
            Long documentId,
            Long indexGeneration,
            String jobType,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待执行任务并固定初始尝试与租约，返回不可变快照。
        return new IndexJobEntity(
                null,
                Ids.next(),
                userId,
                documentId,
                indexGeneration,
                jobType,
                "PENDING",
                0,
                1,
                null,
                null,
                0L,
                null,
                null,
                createdAt,
                updatedAt,
                0L);
    }
}
