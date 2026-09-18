package com.hellotravel.domain.auth.model.entity;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键
 * @param userId 账号归属
 * @param sessionId 已消费刷新令牌所属登录
 * @param tokenHash 仅保留已消费随机令牌摘要，用于重放检测
 * @param createdAt 消费时间，UTC
 * @param expiresAt 原登录绝对到期时间
 * @author AIGenerator
 */
public record RefreshReceiptEntity(
        Long id,
        Long userId,
        Long sessionId,
        byte[] tokenHash,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime expiresAt) {

    /**
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public RefreshReceiptEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
        tokenHash = tokenHash == null ? null : tokenHash.clone();
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "RefreshReceiptEntity{redacted}";
    }

    /**
     * 处理tokenHash对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] tokenHash() {
        return tokenHash == null ? null : tokenHash.clone();
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(RefreshReceiptEntity prior) {
        // 1. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 保留已消费刷新摘要与原会话绑定用于重放识别，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param sessionId 已消费刷新令牌所属登录
     * @param tokenHash 仅保留已消费随机令牌摘要，用于重放检测
     * @param createdAt 创建时间
     * @param expiresAt 有时间适用性的事实到期时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static RefreshReceiptEntity consumed(
            Long userId,
            Long sessionId,
            byte[] tokenHash,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime expiresAt) {
        // 1. 保留已消费刷新摘要与原会话绑定用于重放识别，返回不可变快照。
        return new RefreshReceiptEntity(null, userId, sessionId, tokenHash, createdAt, expiresAt);
    }
}
