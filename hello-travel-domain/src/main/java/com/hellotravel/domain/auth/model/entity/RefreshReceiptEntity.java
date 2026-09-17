package com.hellotravel.domain.auth.model.entity;

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
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public RefreshReceiptEntity {
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
}
