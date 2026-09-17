package com.hellotravel.domain.memory.model.entity;

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
}
