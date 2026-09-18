package com.hellotravel.domain.sync.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record SyncEventAggregate(SyncEventEntity entity) {

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
    public void assertWritableAgainst(SyncEventAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 创建绑定用户提交序号的持久同步事件；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param eventSeq 该用户的连续提交序号；来自账号sync_seq
     * @param eventType 推送、开始生成、提取记忆或验证码投递等事件
     * @param aggregatePublicId 事件目标对外ID
     * @param aggregateVersion 目标实体版本，避免重放覆盖新状态
     * @param targetSessionPublicId 定向踢登录会话事件的目标
     * @param payloadJson 只存受限任务引用，验证码密文在challenge表
     * @param createdAt 创建时间
     * @param expiresAt 有时间适用性的事实到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static SyncEventAggregate committed(
            Long userId,
            Long eventSeq,
            String eventType,
            String aggregatePublicId,
            Long aggregateVersion,
            String targetSessionPublicId,
            String payloadJson,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime expiresAt) {
        // 1. 创建绑定用户提交序号的持久同步事件，具体变更委托实体。
        return new SyncEventAggregate(
                SyncEventEntity.committed(
                        userId,
                        eventSeq,
                        eventType,
                        aggregatePublicId,
                        aggregateVersion,
                        targetSessionPublicId,
                        payloadJson,
                        createdAt,
                        expiresAt));
    }
}
