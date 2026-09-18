package com.hellotravel.domain.memory.model.entity;

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

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(MemorySummaryEntity prior) {
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
     * 清除已失效记忆的派生正文，保留审计及删除状态，固定状态由实体封装。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MemorySummaryEntity redacted() {
        // 1. 清除已失效记忆的派生正文，保留审计及删除状态，返回不可变快照。
        return new MemorySummaryEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.memoryEpoch(),
                this.coveredFromSeq(),
                this.coveredThroughSeq(),
                "{}",
                this.estimatedTokens(),
                this.estimatorVersion(),
                this.modelName(),
                this.promptRevision(),
                "INVALIDATED",
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 创建绑定对话来源范围和记忆代次的摘要，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param conversationId 一期禁止跨对话共享长期事实
     * @param memoryEpoch 当前有效记忆代次
     * @param coveredFromSeq 摘要来源最小消息序号
     * @param coveredThroughSeq 摘要来源最大消息序号
     * @param structuredContent 有界摘要，包含约束、事实、问题、引用及来源序号
     * @param estimatedTokens 摘要token估算，非供应商实际值
     * @param modelName 摘要使用的模型名称
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MemorySummaryEntity compressed(
            Long userId,
            Long conversationId,
            Long memoryEpoch,
            Long coveredFromSeq,
            Long coveredThroughSeq,
            String structuredContent,
            Integer estimatedTokens,
            String modelName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建绑定对话来源范围和记忆代次的摘要，返回不可变快照。
        return new MemorySummaryEntity(
                null,
                Ids.next(),
                userId,
                conversationId,
                memoryEpoch,
                coveredFromSeq,
                coveredThroughSeq,
                structuredContent,
                estimatedTokens,
                "utf8-upper-v1",
                modelName,
                "summary-v1",
                "ACTIVE",
                createdAt,
                updatedAt,
                0L);
    }
}
