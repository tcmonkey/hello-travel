package com.hellotravel.domain.auth.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param emailNormalized 应用规范化后的唯一邮箱
 * @param passwordHash 带算法和参数的加盐密码哈希，不存明文
 * @param emailVerifiedAt 邮箱验证成功时间
 * @param status 账号状态
 * @param authEpoch 全设备撤销版本，重置密码时递增
 * @param syncSeq 按用户分配并提交的持久同步序号
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record UserAccountEntity(
        Long id,
        String publicId,
        String emailNormalized,
        String passwordHash,
        java.time.LocalDateTime emailVerifiedAt,
        String status,
        Long authEpoch,
        Long syncSeq,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 推进账号提交序号，调用方须在同一事务内提交同步事件。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public UserAccountEntity advanceSync() {
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("account inactive");
        }
        return new UserAccountEntity(
                this.id(),
                this.publicId(),
                this.emailNormalized(),
                this.passwordHash(),
                this.emailVerifiedAt(),
                this.status(),
                this.authEpoch(),
                Math.addExact(syncSeq, 1),
                this.createdAt(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.version());
    }

    /**
     * 变更密码摘要并提高认证代次。
     *
     * @author AIGenerator
     * @param hash 受控hash参数
     * @return 当前操作的业务结果
     */
    public UserAccountEntity resetPassword(String hash) {
        return new UserAccountEntity(
                this.id(),
                this.publicId(),
                this.emailNormalized(),
                hash,
                this.emailVerifiedAt(),
                this.status(),
                Math.addExact(authEpoch, 1),
                this.syncSeq(),
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
        return "UserAccountEntity{redacted}";
    }
}
