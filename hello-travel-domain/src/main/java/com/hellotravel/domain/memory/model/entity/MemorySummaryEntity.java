package com.hellotravel.domain.memory.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param conversationId 对话归属
 * @param memoryEpoch 与对话代次匹配才可使用
 * @param coveredFromSeq 摘要来源最小消息序号
 * @param coveredThroughSeq 摘要来源最大消息序号
 * @param structuredContent 有界摘要，包含约束、事实、问题、引用及来源序号
 * @param estimatedTokens 摘要token估算，非供应商实际值
 * @param estimatorVersion 估算方法版本
 * @param modelName 摘要使用的模型名称
 * @param promptRevision 压缩提示模板版本
 * @param status 摘要是否仍有效
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record MemorySummaryEntity(
        Long id,
        String publicId,
        Long userId,
        Long conversationId,
        Long memoryEpoch,
        Long coveredFromSeq,
        Long coveredThroughSeq,
        String structuredContent,
        Integer estimatedTokens,
        String estimatorVersion,
        String modelName,
        String promptRevision,
        String status,
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
        return "MemorySummaryEntity{redacted}";
    }
}
