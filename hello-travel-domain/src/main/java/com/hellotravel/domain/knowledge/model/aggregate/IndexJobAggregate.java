package com.hellotravel.domain.knowledge.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record IndexJobAggregate(IndexJobEntity entity) {

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
    public void assertWritableAgainst(IndexJobAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 领取待执行任务并提高租约栅栏；聚合委托实体，不重复存储标量状态。
     *
     * @param owner owner业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public IndexJobAggregate claim(String owner, java.time.LocalDateTime time) {
        // 1. 领取待执行任务并提高租约栅栏，具体变更委托实体。
        return new IndexJobAggregate(entity.claim(owner, time));
    }

    /**
     * 完成本次尝试并释放活动租约；聚合委托实体，不重复存储标量状态。
     *
     * @param success success业务参数
     * @param error error业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public IndexJobAggregate complete(boolean success, String error) {
        // 1. 完成本次尝试并释放活动租约，具体变更委托实体。
        return new IndexJobAggregate(entity.complete(success, error));
    }

    /**
     * 创建待执行任务并固定初始尝试与租约；聚合委托实体，不重复存储标量状态。
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
    public static IndexJobAggregate pending(
            Long userId,
            Long documentId,
            Long indexGeneration,
            String jobType,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待执行任务并固定初始尝试与租约，具体变更委托实体。
        return new IndexJobAggregate(
                IndexJobEntity.pending(
                        userId, documentId, indexGeneration, jobType, createdAt, updatedAt));
    }
}
