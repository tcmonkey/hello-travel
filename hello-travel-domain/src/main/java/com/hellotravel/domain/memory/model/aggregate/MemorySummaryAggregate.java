package com.hellotravel.domain.memory.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.entity.MemorySummaryEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MemorySummaryAggregate(MemorySummaryEntity entity) {

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
    public void assertWritableAgainst(MemorySummaryAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 清除已失效记忆的派生正文，保留审计及删除状态；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MemorySummaryAggregate redacted() {
        // 1. 清除已失效记忆的派生正文，保留审计及删除状态，具体变更委托实体。
        return new MemorySummaryAggregate(entity.redacted());
    }

    /**
     * 创建绑定对话来源范围和记忆代次的摘要；聚合委托实体，不重复存储标量状态。
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
    public static MemorySummaryAggregate compressed(
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
        // 1. 创建绑定对话来源范围和记忆代次的摘要，具体变更委托实体。
        return new MemorySummaryAggregate(
                MemorySummaryEntity.compressed(
                        userId,
                        conversationId,
                        memoryEpoch,
                        coveredFromSeq,
                        coveredThroughSeq,
                        structuredContent,
                        estimatedTokens,
                        modelName,
                        createdAt,
                        updatedAt));
    }
}
