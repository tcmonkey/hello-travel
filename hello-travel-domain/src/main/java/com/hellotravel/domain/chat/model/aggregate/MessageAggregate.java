package com.hellotravel.domain.chat.model.aggregate;

import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MessageAggregate(MessageEntity entity) {

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
    public void assertWritableAgainst(MessageAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 更新未删除助手消息的正文、状态及已核验引用；聚合委托实体，不重复存储标量状态。
     *
     * @param text text业务参数
     * @param nextStatus nextStatus业务参数
     * @param citations citations业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MessageAggregate progress(String text, String nextStatus, String citations) {
        // 1. 更新未删除助手消息的正文、状态及已核验引用，具体变更委托实体。
        return new MessageAggregate(entity.progress(text, nextStatus, citations));
    }

    /**
     * 清空消息正文与引用并记录删除时间；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MessageAggregate erase() {
        // 1. 清空消息正文与引用并记录删除时间，具体变更委托实体。
        return new MessageAggregate(entity.erase());
    }

    /**
     * 创建绑定会话归属及本轮顺序的用户输入；聚合委托实体，不重复存储标量状态。
     *
     * @param conversation conversation业务参数
     * @param text text业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MessageAggregate userInput(
            ConversationEntity conversation, String text, java.time.LocalDateTime time) {
        // 1. 创建绑定会话归属及本轮顺序的用户输入，具体变更委托实体。
        return new MessageAggregate(MessageEntity.userInput(conversation, text, time));
    }

    /**
     * 为本轮回复预留稳定序号与空正文；聚合委托实体，不重复存储标量状态。
     *
     * @param conversation conversation业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MessageAggregate assistantPlaceholder(
            ConversationEntity conversation, java.time.LocalDateTime time) {
        // 1. 为本轮回复预留稳定序号与空正文，具体变更委托实体。
        return new MessageAggregate(MessageEntity.assistantPlaceholder(conversation, time));
    }
}
