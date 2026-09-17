package com.hellotravel.domain.chat.model.entity;

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
}
