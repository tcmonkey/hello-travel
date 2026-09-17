package com.hellotravel.domain.chat.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属，所有访问必须校验
 * @param title 对话标题，作为纯文本展示
 * @param lastMessageSeq 已分配的最大消息序号
 * @param historyEpoch 消息删除代次；流式内容更新不递增
 * @param memoryEpoch 删除或重建时递增，隔离过期派生记忆
 * @param lastActivityAt 最近活动时间，侧栏排序依据
 * @param deletedAt 逻辑删除时间，删除后禁止推理及查询
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record ConversationEntity(
        Long id,
        String publicId,
        Long userId,
        String title,
        Long lastMessageSeq,
        Long historyEpoch,
        Long memoryEpoch,
        java.time.LocalDateTime lastActivityAt,
        java.time.LocalDateTime deletedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 为一轮用户消息和助手回复分配两个稳定序号。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public ConversationEntity appendPair() {
        if (deletedAt != null) {
            throw new IllegalStateException("deleted");
        }
        return new ConversationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.title(),
                Math.addExact(lastMessageSeq, 2),
                this.historyEpoch(),
                this.memoryEpoch(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.deletedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 验证并变更对话标题。
     *
     * @author AIGenerator
     * @param name 受控name参数
     * @return 当前操作的业务结果
     */
    public ConversationEntity rename(String name) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("title");
        }
        return new ConversationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                name.strip(),
                this.lastMessageSeq(),
                this.historyEpoch(),
                this.memoryEpoch(),
                this.lastActivityAt(),
                this.deletedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 提高删除与记忆代次，使旧来源立即失效。
     *
     * @author AIGenerator
     * @param entire 受控entire参数
     * @return 当前操作的业务结果
     */
    public ConversationEntity eraseHistory(boolean entire) {
        return new ConversationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.title(),
                this.lastMessageSeq(),
                Math.addExact(historyEpoch, 1),
                Math.addExact(memoryEpoch, 1),
                this.lastActivityAt(),
                entire ? java.time.LocalDateTime.now(java.time.ZoneOffset.UTC) : deletedAt,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "ConversationEntity{redacted}";
    }
}
