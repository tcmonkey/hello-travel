package com.hellotravel.domain.chat.model.aggregate;

import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record ConversationAggregate(ConversationEntity entity) {

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
    public void assertWritableAgainst(ConversationAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 为一轮输入与回复分配两个稳定消息序号；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ConversationAggregate appendPair() {
        // 1. 为一轮输入与回复分配两个稳定消息序号，具体变更委托实体。
        return new ConversationAggregate(entity.appendPair());
    }

    /**
     * 变更已验证的对话标题，归属和消息顺序保持不变；聚合委托实体，不重复存储标量状态。
     *
     * @param name name业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ConversationAggregate rename(String name) {
        // 1. 变更已验证的对话标题，归属和消息顺序保持不变，具体变更委托实体。
        return new ConversationAggregate(entity.rename(name));
    }

    /**
     * 提高历史和记忆代次，使旧分页与派生记忆立即失效；聚合委托实体，不重复存储标量状态。
     *
     * @param entire entire业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ConversationAggregate eraseHistory(boolean entire) {
        // 1. 提高历史和记忆代次，使旧分页与派生记忆立即失效，具体变更委托实体。
        return new ConversationAggregate(entity.eraseHistory(entire));
    }

    /**
     * 创建本场景初始快照，固定初始状态；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param title 对话标题，作为纯文本展示
     * @param lastActivityAt 最近活动时间，侧栏排序依据
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static ConversationAggregate started(
            Long userId,
            String title,
            java.time.LocalDateTime lastActivityAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建本场景初始快照，固定初始状态，具体变更委托实体。
        return new ConversationAggregate(
                ConversationEntity.started(userId, title, lastActivityAt, createdAt, updatedAt));
    }
}
