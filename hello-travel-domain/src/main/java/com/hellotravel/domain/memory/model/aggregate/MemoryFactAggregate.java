package com.hellotravel.domain.memory.model.aggregate;

import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.entity.MemoryFactEntity;
import com.hellotravel.domain.memory.model.value.FactProposalValue;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record MemoryFactAggregate(MemoryFactEntity entity) {

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
    public void assertWritableAgainst(MemoryFactAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 替换当前事实与原文证据，保留归属和CAS版本；聚合委托实体，不重复存储标量状态。
     *
     * @param proposal proposal业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MemoryFactAggregate reviseExplicit(
            FactProposalValue proposal, java.time.LocalDateTime time) {
        // 1. 替换当前事实与原文证据，保留归属和CAS版本，具体变更委托实体。
        return new MemoryFactAggregate(entity.reviseExplicit(proposal, time));
    }

    /**
     * 创建只依据用户明确原文的长期事实；聚合委托实体，不重复存储标量状态。
     *
     * @param run run业务参数
     * @param proposal proposal业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MemoryFactAggregate explicit(
            ChatRunEntity run, FactProposalValue proposal, java.time.LocalDateTime time) {
        // 1. 创建只依据用户明确原文的长期事实，具体变更委托实体。
        return new MemoryFactAggregate(MemoryFactEntity.explicit(run, proposal, time));
    }

    /**
     * 核验模型提案的字段边界及用户原文来源；聚合委托实体，不重复存储标量状态。
     *
     * @param key 已验证的稳定事实键
     * @param category 允许的长期事实类别
     * @param excerpt 用户原文连续摘录
     * @param original original业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static java.util.Optional<FactProposalValue> propose(
            String key, String category, String excerpt, String original) {
        // 1. 核验模型提案的字段边界及用户原文来源，具体变更委托实体。
        return MemoryFactEntity.propose(key, category, excerpt, original);
    }

    /**
     * 清除已失效记忆的派生正文，保留审计及删除状态；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MemoryFactAggregate redacted() {
        // 1. 清除已失效记忆的派生正文，保留审计及删除状态，具体变更委托实体。
        return new MemoryFactAggregate(entity.redacted());
    }
}
