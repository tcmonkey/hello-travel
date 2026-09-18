package com.hellotravel.domain.knowledge.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeChunkEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record KnowledgeChunkAggregate(KnowledgeChunkEntity entity) {

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
    public void assertWritableAgainst(KnowledgeChunkAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 创建待执行任务并固定初始尝试与租约；聚合委托实体，不重复存储标量状态。
     *
     * @param job job业务参数
     * @param number number业务参数
     * @param body body业务参数
     * @param hash hash业务参数
     * @param tokens tokens业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static KnowledgeChunkAggregate pending(
            IndexJobEntity job,
            int number,
            String body,
            byte[] hash,
            int tokens,
            java.time.LocalDateTime time) {
        // 1. 创建待执行任务并固定初始尝试与租约，具体变更委托实体。
        return new KnowledgeChunkAggregate(
                KnowledgeChunkEntity.pending(job, number, body, hash, tokens, time));
    }

    /**
     * 完成分块索引，保持正文摘要与向量键不变；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeChunkAggregate ready() {
        // 1. 完成分块索引，保持正文摘要与向量键不变，具体变更委托实体。
        return new KnowledgeChunkAggregate(entity.ready());
    }

    /**
     * 清除过期知识正文并标记删除；聚合委托实体，不重复存储标量状态。
     *
     * @param deletedAt 逻辑删除时间，后续不得参与记忆或检索
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeChunkAggregate deleted(java.time.LocalDateTime deletedAt) {
        // 1. 清除过期知识正文并标记删除，具体变更委托实体。
        return new KnowledgeChunkAggregate(entity.deleted(deletedAt));
    }
}
