package com.hellotravel.domain.chat.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param conversationId 对话归属
 * @param messageSeq 对话内稳定顺序，删除不复用
 * @param role 用户或助手；系统规则不混作用户消息
 * @param status 提交、生成、完成或失败等状态
 * @param content 消息正文；流式草稿可定期保存，受应用大小限制
 * @param citationsJson 已核验的引用片段ID、来源、日期和版本
 * @param deletedAt 逻辑删除时间，后续不得参与记忆或检索
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record MessageEntity(
        Long id,
        String publicId,
        Long userId,
        Long conversationId,
        Long messageSeq,
        String role,
        String status,
        String content,
        String citationsJson,
        java.time.LocalDateTime deletedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 更新助手正文与状态，拒绝向已删除消息写入。
     *
     * @author AIGenerator
     * @param text 有界文本内容
     * @param nextStatus 受控nextStatus参数
     * @param citations 经校验的来源引用
     * @return 当前操作的业务结果
     */
    public MessageEntity progress(String text, String nextStatus, String citations) {
        if (deletedAt != null
                || text == null
                || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1048576) {
            throw new IllegalArgumentException("message");
        }
        return new MessageEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.messageSeq(),
                this.role(),
                nextStatus,
                text,
                citations,
                this.deletedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 清除消息正文和引用并记录删除时间。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public MessageEntity erase() {
        return new MessageEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.conversationId(),
                this.messageSeq(),
                this.role(),
                this.status(),
                "",
                null,
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
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
        return "MessageEntity{redacted}";
    }
}
