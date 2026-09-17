package com.hellotravel.domain.auth.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param deviceId 浏览器设备实例
 * @param accessTokenHash 随机访问令牌哈希，不存原始令牌
 * @param refreshTokenHash 轮换刷新令牌哈希，不存原始令牌
 * @param csrfTokenHash CSRF校验随机值哈希
 * @param authEpoch 签发时账号全局撤销版本
 * @param status 会话状态
 * @param revokeReason 被踢、退出、重置或重放等撤销原因
 * @param accessExpiresAt 访问令牌到期时间
 * @param refreshExpiresAt 刷新令牌到期时间
 * @param lastSeenAt 最近活跃时间
 * @param revokedAt 撤销时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record LoginSessionEntity(
        Long id,
        String publicId,
        Long userId,
        Long deviceId,
        byte[] accessTokenHash,
        byte[] refreshTokenHash,
        byte[] csrfTokenHash,
        Long authEpoch,
        String status,
        String revokeReason,
        java.time.LocalDateTime accessExpiresAt,
        java.time.LocalDateTime refreshExpiresAt,
        java.time.LocalDateTime lastSeenAt,
        java.time.LocalDateTime revokedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public LoginSessionEntity {
        accessTokenHash = accessTokenHash == null ? null : accessTokenHash.clone();
        refreshTokenHash = refreshTokenHash == null ? null : refreshTokenHash.clone();
        csrfTokenHash = csrfTokenHash == null ? null : csrfTokenHash.clone();
    }

    /**
     * 撤销登录并清除活动令牌摘要。
     *
     * @author AIGenerator
     * @param reason 受控reason参数
     * @return 当前操作的业务结果
     */
    public LoginSessionEntity revoke(String reason) {
        return new LoginSessionEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.deviceId(),
                null,
                null,
                null,
                this.authEpoch(),
                "REVOKED",
                reason,
                this.accessExpiresAt(),
                this.refreshExpiresAt(),
                this.lastSeenAt(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 轮换活动登录的访问与刷新凭据。
     *
     * @author AIGenerator
     * @param access 受控access参数
     * @param refresh 受控refresh参数
     * @param csrf 受控csrf参数
     * @param until 受控until参数
     * @return 当前操作的业务结果
     */
    public LoginSessionEntity rotate(
            byte[] access, byte[] refresh, byte[] csrf, java.time.LocalDateTime until) {
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("revoked");
        }
        return new LoginSessionEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.deviceId(),
                access,
                refresh,
                csrf,
                this.authEpoch(),
                this.status(),
                this.revokeReason(),
                until,
                this.refreshExpiresAt(),
                this.lastSeenAt(),
                this.revokedAt(),
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
        return "LoginSessionEntity{redacted}";
    }

    /**
     * 处理accessTokenHash对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] accessTokenHash() {
        return accessTokenHash == null ? null : accessTokenHash.clone();
    }

    /**
     * 处理refreshTokenHash对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] refreshTokenHash() {
        return refreshTokenHash == null ? null : refreshTokenHash.clone();
    }

    /**
     * 处理csrfTokenHash对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] csrfTokenHash() {
        return csrfTokenHash == null ? null : csrfTokenHash.clone();
    }
}
