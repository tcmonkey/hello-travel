package com.hellotravel.domain.auth.model.aggregate;

import com.hellotravel.domain.auth.model.entity.RefreshReceiptEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record RefreshReceiptAggregate(RefreshReceiptEntity entity) {

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
    public void assertWritableAgainst(RefreshReceiptAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 保留已消费刷新摘要与原会话绑定用于重放识别；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param sessionId 已消费刷新令牌所属登录
     * @param tokenHash 仅保留已消费随机令牌摘要，用于重放检测
     * @param createdAt 创建时间
     * @param expiresAt 有时间适用性的事实到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static RefreshReceiptAggregate consumed(
            Long userId,
            Long sessionId,
            byte[] tokenHash,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime expiresAt) {
        // 1. 保留已消费刷新摘要与原会话绑定用于重放识别，具体变更委托实体。
        return new RefreshReceiptAggregate(
                RefreshReceiptEntity.consumed(userId, sessionId, tokenHash, createdAt, expiresAt));
    }
}
