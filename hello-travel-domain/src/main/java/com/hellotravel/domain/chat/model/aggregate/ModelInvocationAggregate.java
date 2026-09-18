package com.hellotravel.domain.chat.model.aggregate;

import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record ModelInvocationAggregate(ModelInvocationEntity entity) {

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
    public void assertWritableAgainst(ModelInvocationAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 记录提取结果和真实usage，任务重放不再计费；聚合委托实体，不重复存储标量状态。
     *
     * @param modelName 摘要使用的模型名称
     * @param actualInputTokens 供应商确实返回时记录实际输入用量
     * @param actualOutputTokens 供应商确实返回时记录实际输出用量
     * @param status 有效、失效或到期
     * @param completedAt 终态时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ModelInvocationAggregate extractionCompleted(
            String modelName,
            Integer actualInputTokens,
            Integer actualOutputTokens,
            String status,
            java.time.LocalDateTime completedAt) {
        // 1. 记录提取结果和真实usage，任务重放不再计费，具体变更委托实体。
        return new ModelInvocationAggregate(
                entity.extractionCompleted(
                        modelName, actualInputTokens, actualOutputTokens, status, completedAt));
    }

    /**
     * 创建提取调用证据，先登记再调用模型；聚合委托实体，不重复存储标量状态。
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
    public static ModelInvocationAggregate extractionStarted(
            Long userId,
            Long conversationId,
            Long runId,
            Integer runAttemptNo,
            Integer estimatedInputTokens,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建提取调用证据，先登记再调用模型，具体变更委托实体。
        return new ModelInvocationAggregate(
                ModelInvocationEntity.extractionStarted(
                        userId,
                        conversationId,
                        runId,
                        runAttemptNo,
                        estimatedInputTokens,
                        createdAt,
                        updatedAt));
    }

    /**
     * 创建本场景初始快照，固定初始状态；聚合委托实体，不重复存储标量状态。
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
    public static ModelInvocationAggregate started(
            Long userId,
            Long conversationId,
            Long runId,
            String stage,
            Integer runAttemptNo,
            Integer attemptNo,
            Integer estimatedInputTokens,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建本场景初始快照，固定初始状态，具体变更委托实体。
        return new ModelInvocationAggregate(
                ModelInvocationEntity.started(
                        userId,
                        conversationId,
                        runId,
                        stage,
                        runAttemptNo,
                        attemptNo,
                        estimatedInputTokens,
                        createdAt,
                        updatedAt));
    }

    /**
     * 记录当前处理结果并保持原始归属与版本；聚合委托实体，不重复存储标量状态。
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
    public ModelInvocationAggregate completed(
            String modelName,
            Integer actualInputTokens,
            Integer actualOutputTokens,
            Integer latencyMs,
            String providerRequestId,
            String status,
            String errorCode,
            java.time.LocalDateTime completedAt) {
        // 1. 记录当前处理结果并保持原始归属与版本，具体变更委托实体。
        return new ModelInvocationAggregate(
                entity.completed(
                        modelName,
                        actualInputTokens,
                        actualOutputTokens,
                        latencyMs,
                        providerRequestId,
                        status,
                        errorCode,
                        completedAt));
    }
}
