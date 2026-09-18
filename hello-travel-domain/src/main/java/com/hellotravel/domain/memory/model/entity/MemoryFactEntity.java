package com.hellotravel.domain.memory.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.memory.model.value.FactProposalValue;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param conversationId 一期禁止跨对话共享长期事实
 * @param memoryEpoch 当前有效记忆代次
 * @param factKey 稳定语义键，如出行预算，不由前端决定归属
 * @param category 偏好、限制或已确认计划
 * @param content 有界事实文本，禁止模型推测冒充用户确认
 * @param evidenceType 一期只采纳用户明确陈述
 * @param sourceCount 来源数，使用前必须验证至少一个有效来源
 * @param status 有效、失效或到期
 * @param expiresAt 有时间适用性的事实到期时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record MemoryFactEntity(
        Long id,
        String publicId,
        Long userId,
        Long conversationId,
        Long memoryEpoch,
        String factKey,
        String category,
        String content,
        String evidenceType,
        Integer sourceCount,
        String status,
        java.time.LocalDateTime expiresAt,
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
        return "MemoryFactEntity{redacted}";
    }

    /**
     * 用已校验的用户原文提案更新事实，保留归属、代次和CAS版本。
     *
     * @param proposal 已核验的用户原文提案
     * @param time 当前UTC时间
     * @return 更新后的事实快照
     * @author AIGenerator
     */
    public MemoryFactEntity reviseExplicit(
            FactProposalValue proposal, java.time.LocalDateTime time) {
        // 1. 替换当前事实与原文证据，保留归属和CAS版本，返回不可变快照。
        return new MemoryFactEntity(
                id,
                publicId,
                userId,
                conversationId,
                memoryEpoch,
                proposal.key(),
                proposal.category(),
                proposal.excerpt(),
                "USER_EXPLICIT",
                1,
                "ACTIVE",
                time.plusDays(90),
                createdAt,
                time,
                version);
    }

    /**
     * 创建绑定当前用户、会话和记忆代次的显式事实。
     *
     * @param run 来源生成任务
     * @param proposal 已核验的用户原文提案
     * @param time 当前UTC时间
     * @return 新事实快照
     * @author AIGenerator
     */
    public static MemoryFactEntity explicit(
            ChatRunEntity run, FactProposalValue proposal, java.time.LocalDateTime time) {
        // 1. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        String publicId = Ids.next();
        // 2. 创建只依据用户明确原文的长期事实，返回不可变快照。
        return new MemoryFactEntity(
                null,
                publicId,
                run.userId(),
                run.conversationId(),
                run.memoryEpochAtStart(),
                proposal.key(),
                proposal.category(),
                proposal.excerpt(),
                "USER_EXPLICIT",
                1,
                "ACTIVE",
                time.plusDays(90),
                time,
                time,
                0L);
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(MemoryFactEntity prior) {
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
     * 从用户原文核验长期事实提案，结构和来源规则由值对象完成。
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
        // 1. 核验模型提案的字段边界及用户原文来源，返回不可变快照。
        return FactProposalValue.accept(key, category, excerpt, original);
    }

    /**
     * 清除已失效记忆的派生正文，保留审计及删除状态，固定状态由实体封装。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public MemoryFactEntity redacted() {
        // 1. 清除已失效记忆的派生正文，保留审计及删除状态，返回不可变快照。
        return new MemoryFactEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.memoryEpoch(),
                this.factKey(),
                this.category(),
                "",
                this.evidenceType(),
                0,
                "INVALIDATED",
                this.expiresAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }
}
