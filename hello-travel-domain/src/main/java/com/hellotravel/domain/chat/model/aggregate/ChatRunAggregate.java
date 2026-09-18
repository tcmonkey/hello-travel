package com.hellotravel.domain.chat.model.aggregate;

import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record ChatRunAggregate(ChatRunEntity entity) {

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
    public void assertWritableAgainst(ChatRunAggregate stored) {
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
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ChatRunAggregate claim(String owner) {
        // 1. 领取待执行任务并提高租约栅栏，具体变更委托实体。
        return new ChatRunAggregate(entity.claim(owner));
    }

    /**
     * 结束当前生成尝试，终态不自动重复调用模型；聚合委托实体，不重复存储标量状态。
     *
     * @param terminal terminal业务参数
     * @param error error业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ChatRunAggregate finish(String terminal, String error) {
        // 1. 结束当前生成尝试，终态不自动重复调用模型，具体变更委托实体。
        return new ChatRunAggregate(entity.finish(terminal, error));
    }

    /**
     * 启动显式重试代次，复用原始消息而不再次提交用户输入；聚合委托实体，不重复存储标量状态。
     *
     * @param epoch epoch业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ChatRunAggregate retry(long epoch) {
        // 1. 启动显式重试代次，复用原始消息而不再次提交用户输入，具体变更委托实体。
        return new ChatRunAggregate(entity.retry(epoch));
    }

    /**
     * 保留当前图节点与预算，旧执行者须通过栅栏检查；聚合委托实体，不重复存储标量状态。
     *
     * @param node node业务参数
     * @param data data业务参数
     * @param budget budget业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public ChatRunAggregate checkpoint(String node, String data, String budget) {
        // 1. 保留当前图节点与预算，旧执行者须通过栅栏检查，具体变更委托实体。
        return new ChatRunAggregate(entity.checkpoint(node, data, budget));
    }

    /**
     * 接受绑定消息对、幂等键和记忆代次的生成任务；聚合委托实体，不重复存储标量状态。
     *
     * @param conversation conversation业务参数
     * @param sessionId 已消费刷新令牌所属登录
     * @param requestKey 客户端幂等请求UUID
     * @param digest digest业务参数
     * @param input 用户输入或所属用例的可信原始数据
     * @param output 模型回复预留预算
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static ChatRunAggregate accepted(
            ConversationEntity conversation,
            Long sessionId,
            String requestKey,
            byte[] digest,
            MessageEntity input,
            MessageEntity output,
            java.time.LocalDateTime time) {
        // 1. 接受绑定消息对、幂等键和记忆代次的生成任务，具体变更委托实体。
        return new ChatRunAggregate(
                ChatRunEntity.accepted(
                        conversation, sessionId, requestKey, digest, input, output, time));
    }

    /**
     * 创建本次生成任务的上下文预算，容量和预留不变量由领域值对象控制。
     *
     * @param window 应用模型窗口
     * @param input 当前完整输入的保守估算
     * @param output 输出预留
     * @param safety 安全预留
     * @return 本次任务预算
     * @author AIGenerator
     */
    public ContextBudgetValue contextBudget(int window, int input, int output, int safety) {
        // 1. 封装当前任务的预算，应用编排只读取容量与压缩决策。
        return entity.contextBudget(window, input, output, safety);
    }
}
