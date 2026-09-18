package com.hellotravel.domain.chat.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param conversationId 对话归属
 * @param runId 生成任务标识
 * @param stage 意图、压缩、回答或记忆提取阶段
 * @param runAttemptNo 所属生成尝试代次；对应run的attempt_count
 * @param attemptNo 本生成尝试内该阶段调用编号
 * @param modelName 实际配置的模型名称
 * @param promptRevision 提示模板版本
 * @param estimatedInputTokens 发送前保守输入估算
 * @param estimatorVersion 估算方法和版本
 * @param actualInputTokens 供应商确实返回时记录实际输入用量
 * @param actualOutputTokens 供应商确实返回时记录实际输出用量
 * @param latencyMs 调用耗时
 * @param providerRequestId 供应商提供的关联标识，禁止写认证头
 * @param status 调用状态
 * @param errorCode 内部稳定故障分类
 * @param completedAt 调用结束时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record ModelInvocationEntity(
        Long id,
        String publicId,
        Long userId,
        Long conversationId,
        Long runId,
        String stage,
        Integer runAttemptNo,
        Integer attemptNo,
        String modelName,
        String promptRevision,
        Integer estimatedInputTokens,
        String estimatorVersion,
        Integer actualInputTokens,
        Integer actualOutputTokens,
        Integer latencyMs,
        String providerRequestId,
        String status,
        String errorCode,
        java.time.LocalDateTime completedAt,
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
        return "ModelInvocationEntity{redacted}";
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(ModelInvocationEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对会话归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.conversationId(), this.conversationId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 记录提取结果和真实usage，任务重放不再计费，固定状态由实体封装。
     *
     * @param modelName 摘要使用的模型名称
     * @param actualInputTokens 供应商确实返回时记录实际输入用量
     * @param actualOutputTokens 供应商确实返回时记录实际输出用量
     * @param status 有效、失效或到期
     * @param completedAt 终态时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ModelInvocationEntity extractionCompleted(
            String modelName,
            Integer actualInputTokens,
            Integer actualOutputTokens,
            String status,
            java.time.LocalDateTime completedAt) {
        // 1. 记录提取结果和真实usage，任务重放不再计费，返回不可变快照。
        return new ModelInvocationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.runId(),
                this.stage(),
                this.runAttemptNo(),
                this.attemptNo(),
                modelName,
                this.promptRevision(),
                this.estimatedInputTokens(),
                this.estimatorVersion(),
                actualInputTokens,
                actualOutputTokens,
                this.latencyMs(),
                this.providerRequestId(),
                status,
                this.errorCode(),
                completedAt,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 创建提取调用证据，先登记再调用模型，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param conversationId 一期禁止跨对话共享长期事实
     * @param runId 生成任务标识
     * @param runAttemptNo 所属生成尝试代次；对应run的attempt_count
     * @param estimatedInputTokens 发送前保守输入估算
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static ModelInvocationEntity extractionStarted(
            Long userId,
            Long conversationId,
            Long runId,
            Integer runAttemptNo,
            Integer estimatedInputTokens,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建提取调用证据，先登记再调用模型，返回不可变快照。
        return new ModelInvocationEntity(
                null,
                Ids.next(),
                userId,
                conversationId,
                runId,
                "MEMORY_EXTRACTION",
                runAttemptNo,
                1,
                "configured-chat",
                "facts-v1",
                estimatedInputTokens,
                "utf8-upper-v1",
                null,
                null,
                null,
                null,
                "STARTED",
                null,
                null,
                createdAt,
                updatedAt,
                0L);
    }

    /**
     * 创建本场景初始快照，固定初始状态，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param conversationId 一期禁止跨对话共享长期事实
     * @param runId 生成任务标识
     * @param stage 意图、压缩、回答或记忆提取阶段
     * @param runAttemptNo 所属生成尝试代次；对应run的attempt_count
     * @param attemptNo 本生成尝试内该阶段调用编号
     * @param estimatedInputTokens 发送前保守输入估算
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static ModelInvocationEntity started(
            Long userId,
            Long conversationId,
            Long runId,
            String stage,
            Integer runAttemptNo,
            Integer attemptNo,
            Integer estimatedInputTokens,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建本场景初始快照，固定初始状态，返回不可变快照。
        return new ModelInvocationEntity(
                null,
                Ids.next(),
                userId,
                conversationId,
                runId,
                stage,
                runAttemptNo,
                attemptNo,
                "configured-chat",
                "travel-v1",
                estimatedInputTokens,
                "utf8-upper-v1",
                null,
                null,
                null,
                null,
                "STARTED",
                null,
                null,
                createdAt,
                updatedAt,
                0L);
    }

    /**
     * 记录当前处理结果并保持原始归属与版本，固定状态由实体封装。
     *
     * @param modelName 摘要使用的模型名称
     * @param actualInputTokens 供应商确实返回时记录实际输入用量
     * @param actualOutputTokens 供应商确实返回时记录实际输出用量
     * @param latencyMs 调用耗时
     * @param providerRequestId 供应商提供的关联标识，禁止写认证头
     * @param status 有效、失效或到期
     * @param errorCode 内部稳定错误码，不保存原始供应商错误
     * @param completedAt 终态时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ModelInvocationEntity completed(
            String modelName,
            Integer actualInputTokens,
            Integer actualOutputTokens,
            Integer latencyMs,
            String providerRequestId,
            String status,
            String errorCode,
            java.time.LocalDateTime completedAt) {
        // 1. 记录当前处理结果并保持原始归属与版本，返回不可变快照。
        return new ModelInvocationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.runId(),
                this.stage(),
                this.runAttemptNo(),
                this.attemptNo(),
                modelName,
                this.promptRevision(),
                this.estimatedInputTokens(),
                this.estimatorVersion(),
                actualInputTokens,
                actualOutputTokens,
                latencyMs,
                providerRequestId,
                status,
                errorCode,
                completedAt,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }
}
