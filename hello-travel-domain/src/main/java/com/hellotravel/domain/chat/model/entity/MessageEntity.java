package com.hellotravel.domain.chat.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
        if (deletedAt != null
                || text == null
                || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1048576) {
            throw new IllegalArgumentException("message");
        }
        // 2. 更新未删除助手消息的正文、状态及已核验引用，返回不可变快照。
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
        // 1. 清空消息正文与引用并记录删除时间，返回不可变快照。
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

    /**
     * 创建绑定会话归属与稳定序号的用户输入。
     *
     * @param conversation 已校验的会话
     * @param text 用户原文
     * @param time 当前UTC时间
     * @return 新提交的用户消息
     * @author AIGenerator
     */
    public static MessageEntity userInput(
            ConversationEntity conversation, String text, java.time.LocalDateTime time) {
        // 1. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        if (conversation.deletedAt() != null) {
            throw new IllegalStateException("deleted conversation");
        }
        String publicId = Ids.next();
        // 2. 创建绑定会话归属及本轮顺序的用户输入，返回不可变快照。
        return new MessageEntity(
                null,
                publicId,
                conversation.userId(),
                conversation.id(),
                conversation.lastMessageSeq() + 1,
                "USER",
                "COMPLETED",
                text,
                null,
                null,
                time,
                time,
                0L);
    }

    /**
     * 为本轮助手回复预留稳定顺序，不复制用户正文。
     *
     * @param conversation 已校验的会话
     * @param time 当前UTC时间
     * @return 等待生成的助手消息
     * @author AIGenerator
     */
    public static MessageEntity assistantPlaceholder(
            ConversationEntity conversation, java.time.LocalDateTime time) {
        // 1. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        if (conversation.deletedAt() != null) {
            throw new IllegalStateException("deleted conversation");
        }
        String publicId = Ids.next();
        // 2. 为本轮回复预留稳定序号与空正文，返回不可变快照。
        return new MessageEntity(
                null,
                publicId,
                conversation.userId(),
                conversation.id(),
                conversation.lastMessageSeq() + 2,
                "ASSISTANT",
                null,
                "",
                null,
                null,
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
    public void assertUpdateAgainst(MessageEntity prior) {
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
        // 5. 核对已删除快照不能恢复为未删除，不满足时拒绝本次更新。
        if (prior.deletedAt() != null && this.deletedAt() == null) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }
}
