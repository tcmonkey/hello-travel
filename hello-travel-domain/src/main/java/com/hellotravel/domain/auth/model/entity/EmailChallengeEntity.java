package com.hellotravel.domain.auth.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param emailNormalized 目标邮箱，注册前可无账号
 * @param purpose 注册、登录或重置用途
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
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public EmailChallengeEntity {
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
        if (!"ISSUED".equals(status)
                || !expiresAt.isAfter(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            throw new IllegalStateException("challenge unavailable");
        }
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
        int next = Math.min(maxAttempts, attempts + 1);
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
        return codeHmac == null ? null : codeHmac.clone();
    }

    /**
     * 处理deliveryCiphertext对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] deliveryCiphertext() {
        return deliveryCiphertext == null ? null : deliveryCiphertext.clone();
    }
}
