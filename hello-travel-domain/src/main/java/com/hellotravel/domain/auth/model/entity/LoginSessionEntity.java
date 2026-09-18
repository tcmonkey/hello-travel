package com.hellotravel.domain.auth.model.entity;

import com.hellotravel.domain.auth.model.value.SessionCredentialsValue;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public LoginSessionEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
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
        // 1. 撤销活动会话并清除访问、刷新与CSRF摘要，返回不可变快照。
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
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("revoked");
        }
        // 2. 轮换活动会话的随机凭据，保留原刷新有效期，返回不可变快照。
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

    /**
     * 签发设备会话，由实体封装完整凭据摘要与初始有效期。
     *
     * @param user 已验证账号
     * @param device 属于当前账号的设备
     * @param publicId 业务公开标识
     * @param accessHash accessHash业务参数
     * @param refreshHash refreshHash业务参数
     * @param csrfHash csrfHash业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static LoginSessionEntity issue(
            UserAccountEntity user,
            DeviceEntity device,
            String publicId,
            byte[] accessHash,
            byte[] refreshHash,
            byte[] csrfHash,
            java.time.LocalDateTime time) {
        // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
        var credentials = new SessionCredentialsValue(publicId, accessHash, refreshHash, csrfHash);
        // 2. 核对关联对象归属与角色，拒绝跨账号或跨会话关联。
        if (!user.id().equals(device.userId())) {
            throw new IllegalArgumentException("device ownership");
        }
        // 3. 签发浏览器设备的新会话并固定认证代次及有效期，返回不可变快照。
        return new LoginSessionEntity(
                null,
                credentials.publicId(),
                user.id(),
                device.id(),
                credentials.accessHash(),
                credentials.refreshHash(),
                credentials.csrfHash(),
                user.authEpoch(),
                "ACTIVE",
                null,
                time.plusMinutes(15),
                time.plusDays(7),
                time,
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
    public void assertUpdateAgainst(LoginSessionEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对设备归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.deviceId(), this.deviceId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }
}
