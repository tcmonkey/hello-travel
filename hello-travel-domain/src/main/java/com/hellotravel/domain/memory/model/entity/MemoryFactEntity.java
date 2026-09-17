package com.hellotravel.domain.memory.model.entity;

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
}
