package com.hellotravel.domain.auth.model.entity;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param emailNormalized 目标邮箱，登录码签发时可无账号
 * @param purpose 登录或密码设置/重置用途
 * @param codeHmac 含挑战ID、邮箱和用途的验证码HMAC
 * @param codeKeyVersion 验证码校验密钥版本标识，无密钥值
 * @param deliveryCiphertext 待发送验证码AEAD密文封装，发送后清除
 * @param deliveryKeyVersion 发送密钥版本标识，独立于校验密钥
 * @param status 投递及消费状态
 * @param attempts 失败验证次数
 * @param maxAttempts 允许的失败次数上限
 * @param expiresAt 验证码到期时间
 * @param consumedAt 成功消费时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record EmailChallengeEntity(
        Long id,
        String publicId,
        String emailNormalized,
        String purpose,
        byte[] codeHmac,
        String codeKeyVersion,
        byte[] deliveryCiphertext,
        String deliveryKeyVersion,
        String status,
        Integer attempts,
        Integer maxAttempts,
        java.time.LocalDateTime expiresAt,
        java.time.LocalDateTime consumedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public EmailChallengeEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
        codeHmac = codeHmac == null ? null : codeHmac.clone();
        deliveryCiphertext = deliveryCiphertext == null ? null : deliveryCiphertext.clone();
    }

    /**
     * 消费未到期的已发送邮箱验证请求。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public EmailChallengeEntity consume() {
        // 1. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"ISSUED".equals(status)
                || !expiresAt.isAfter(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalStateException("challenge unavailable");
        }
        // 2. 消费当前验证码并清除可投递载荷，返回不可变快照。
        return new EmailChallengeEntity(
                this.id(),
                this.publicId(),
                this.emailNormalized(),
                this.purpose(),
                this.codeHmac(),
                this.codeKeyVersion(),
                null,
                this.deliveryKeyVersion(),
                "CONSUMED",
                this.attempts(),
                this.maxAttempts(),
                this.expiresAt(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 记录验证失败次数并在达到上限后失效。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public EmailChallengeEntity failedAttempt() {
        // 1. 准备当前变更后的不可变快照，旧版本保留给仓储CAS。
        int next = Math.min(maxAttempts, attempts + 1);
        // 2. 增加验证失败次数，达到上限后停止接受证明，返回不可变快照。
        return new EmailChallengeEntity(
                this.id(),
                this.publicId(),
                this.emailNormalized(),
                this.purpose(),
                this.codeHmac(),
                this.codeKeyVersion(),
                this.deliveryCiphertext(),
                this.deliveryKeyVersion(),
                next >= maxAttempts ? "FAILED" : status,
                next,
                this.maxAttempts(),
                this.expiresAt(),
                this.consumedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 记录SMTP投递结果并清除可解密验证码。
     *
     * @author AIGenerator
     * @param state 受控state参数
     * @return 当前操作的业务结果
     */
    public EmailChallengeEntity delivery(String state) {
        // 1. 返回变更后的不可变实体，保留未变归属和乐观锁版本。
        return new EmailChallengeEntity(
                this.id(),
                this.publicId(),
                this.emailNormalized(),
                this.purpose(),
                this.codeHmac(),
                this.codeKeyVersion(),
                null,
                this.deliveryKeyVersion(),
                state,
                this.attempts(),
                this.maxAttempts(),
                this.expiresAt(),
                this.consumedAt(),
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
        return "EmailChallengeEntity{redacted}";
    }

    /**
     * 处理codeHmac对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] codeHmac() {
        // 1. 交付摘要的独立副本，调用者修改返回数组不会改变实体快照。
        return codeHmac == null ? null : codeHmac.clone();
    }

    /**
     * 处理deliveryCiphertext对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] deliveryCiphertext() {
        // 1. 交付摘要的独立副本，调用者修改返回数组不会改变实体快照。
        return deliveryCiphertext == null ? null : deliveryCiphertext.clone();
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(EmailChallengeEntity prior) {
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
     * 创建待投递验证码，摘要与加密载荷分开保存，固定状态由实体封装。
     *
     * @param publicId 业务公开标识
     * @param emailNormalized 应用规范化后的唯一邮箱
     * @param purpose 登录或密码设置/重置用途
     * @param codeHmac 含挑战ID、邮箱和用途的验证码HMAC
     * @param deliveryCiphertext 待发送验证码AEAD密文封装，发送后清除
     * @param expiresAt 有时间适用性的事实到期时间
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static EmailChallengeEntity pendingDelivery(
            String publicId,
            String emailNormalized,
            String purpose,
            byte[] codeHmac,
            byte[] deliveryCiphertext,
            java.time.LocalDateTime expiresAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待投递验证码，摘要与加密载荷分开保存，返回不可变快照。
        return new EmailChallengeEntity(
                null,
                publicId,
                emailNormalized,
                purpose,
                codeHmac,
                "v1",
                deliveryCiphertext,
                "v1",
                "PENDING_SEND",
                0,
                5,
                expiresAt,
                null,
                createdAt,
                updatedAt,
                0L);
    }
}
