package com.hellotravel.domain.auth.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("account inactive");
        }
        // 2. 推进同用户提交序号，与事件在同一事务提交，返回不可变快照。
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
        // 1. 更新密码摘要并提高认证代次，旧会话随之失效，返回不可变快照。
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

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(UserAccountEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 创建邮箱已验证的新账号并固定初始认证代次，固定状态由实体封装。
     *
     * @param emailNormalized 应用规范化后的唯一邮箱
     * @param passwordHash 带算法和参数的加盐密码哈希，不存明文
     * @param emailVerifiedAt 邮箱验证成功时间
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static UserAccountEntity registered(
            String emailNormalized,
            String passwordHash,
            java.time.LocalDateTime emailVerifiedAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 拒绝不完整的已验证邮箱和密码摘要，不能先创建无效账号再补填证明。
        if (emailNormalized == null
                || emailNormalized.isBlank()
                || passwordHash == null
                || passwordHash.isBlank()
                || emailVerifiedAt == null) {
            throw new IllegalArgumentException("verified account");
        }
        // 2. 固定活动状态、初始认证代次和同步序号，持久化主键由仓储分配。
        return new UserAccountEntity(
                null,
                Ids.next(),
                emailNormalized,
                passwordHash,
                emailVerifiedAt,
                "ACTIVE",
                0L,
                0L,
                createdAt,
                updatedAt,
                0L);
    }
}
