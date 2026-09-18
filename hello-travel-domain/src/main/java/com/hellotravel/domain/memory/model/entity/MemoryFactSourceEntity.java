package com.hellotravel.domain.memory.model.entity;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键
 * @param userId 账号归属
 * @param conversationId 对话归属
 * @param factId 长期事实标识
 * @param messageId 来源原始用户消息标识
 * @param evidenceExcerpt 可核对的有界证据摘录，来源删除后同步清除
 * @param messageVersion 引用时原消息版本
 * @param createdAt 创建时间，UTC
 * @author AIGenerator
 */
public record MemoryFactSourceEntity(
        Long id,
        Long userId,
        Long conversationId,
        Long factId,
        Long messageId,
        String evidenceExcerpt,
        Long messageVersion,
        java.time.LocalDateTime createdAt) {

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "MemoryFactSourceEntity{redacted}";
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(MemoryFactSourceEntity prior) {
        // 1. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 2. 核对会话归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.conversationId(), this.conversationId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 记录长期事实的原始用户消息与版本证据，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param conversationId 一期禁止跨对话共享长期事实
     * @param factId 长期事实标识
     * @param messageId 来源原始用户消息标识
     * @param evidenceExcerpt 可核对的有界证据摘录，来源删除后同步清除
     * @param messageVersion 引用时原消息版本
     * @param createdAt 创建时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static MemoryFactSourceEntity evidence(
            Long userId,
            Long conversationId,
            Long factId,
            Long messageId,
            String evidenceExcerpt,
            Long messageVersion,
            java.time.LocalDateTime createdAt) {
        // 1. 记录长期事实的原始用户消息与版本证据，返回不可变快照。
        return new MemoryFactSourceEntity(
                null,
                userId,
                conversationId,
                factId,
                messageId,
                evidenceExcerpt,
                messageVersion,
                createdAt);
    }
}
