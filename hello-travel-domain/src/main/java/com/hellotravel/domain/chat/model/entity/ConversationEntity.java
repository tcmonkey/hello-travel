package com.hellotravel.domain.chat.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
        if (deletedAt != null) {
            throw new IllegalStateException("deleted");
        }
        // 2. 为一轮输入与回复分配两个稳定消息序号，返回不可变快照。
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
        // 1. 拒绝空白或超过120字符的标题，保证所有重命名入口共享同一规则。
        String normalized = normalizedTitle(name);
        // 2. 变更已验证的对话标题，归属和消息顺序保持不变，返回不可变快照。
        return new ConversationEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                normalized,
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
        // 1. 提高历史和记忆代次，使旧分页与派生记忆立即失效，返回不可变快照。
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

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(ConversationEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对已删除快照不能恢复为未删除，不满足时拒绝本次更新。
        if (prior.deletedAt() != null && this.deletedAt() == null) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }

    /**
     * 创建本场景初始快照，固定初始状态，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param title 对话标题，作为纯文本展示
     * @param lastActivityAt 最近活动时间，侧栏排序依据
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static ConversationEntity started(
            Long userId,
            String title,
            java.time.LocalDateTime lastActivityAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建入口也复用实体标题规则，避免产生需要立即修正的无效快照。
        String normalized = normalizedTitle(title);
        // 2. 新对话必须属于已确定的账号，归属不由后续更新补填。
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("conversation owner");
        }
        // 3. 固定初始序号和记忆代次，返回满足输入不变量的新快照。
        return new ConversationEntity(
                null,
                Ids.next(),
                userId,
                normalized,
                0L,
                0L,
                0L,
                lastActivityAt,
                null,
                createdAt,
                updatedAt,
                0L);
    }

    /**
     * 创建和重命名共享标题边界，保持领域内唯一规则。
     *
     * @param title 原始标题
     * @return 去除首尾空白的有效标题
     * @author AIGenerator
     */
    private static String normalizedTitle(String title) {
        // 1. 拒绝空白及超长标题，所有业务入口使用同一边界。
        if (title == null || title.isBlank() || title.length() > 120) {
            throw new IllegalArgumentException("title");
        }
        // 2. 返回规范标题，创建和后续变更均不保留首尾空白。
        return title.strip();
    }
}
