package com.hellotravel.domain.auth.model.aggregate;

import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record EmailChallengeAggregate(EmailChallengeEntity entity) {

    /**
     * 校验聚合输入完整性，防止空实体进入业务保存。
     *
     * @author AIGenerator
     */
    public void assertComplete() {
        // 1. 拒绝空实体容器，完整聚合才可以进入保存流程。
        if (entity == null) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 提供仓储加载所需的标识；标量状态仍只由实体持有。
     *
     * @return 已保存标识或新增时空值
     * @author AIGenerator
     */
    public Long idForPersistence() {
        return entity.id();
    }

    /**
     * 委托实体核对已持久化聚合的更新约束，不在领域服务展开实体属性。
     *
     * @param stored 已恢复的聚合
     * @author AIGenerator
     */
    public void assertWritableAgainst(EmailChallengeAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 消费当前验证码并清除可投递载荷；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public EmailChallengeAggregate consume() {
        // 1. 消费当前验证码并清除可投递载荷，具体变更委托实体。
        return new EmailChallengeAggregate(entity.consume());
    }

    /**
     * 增加验证失败次数，达到上限后停止接受证明；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public EmailChallengeAggregate failedAttempt() {
        // 1. 增加验证失败次数，达到上限后停止接受证明，具体变更委托实体。
        return new EmailChallengeAggregate(entity.failedAttempt());
    }

    /**
     * 通过聚合的delivery语义入口委托实体，业务状态不由应用层展开。
     *
     * @param state state业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public EmailChallengeAggregate delivery(String state) {
        // 1. 通过聚合语义入口委托实体，避免应用层展开状态字段。
        return new EmailChallengeAggregate(entity.delivery(state));
    }

    /**
     * 创建待投递验证码，摘要与加密载荷分开保存；聚合委托实体，不重复存储标量状态。
     *
     * @param publicId 业务公开标识
     * @param emailNormalized 应用规范化后的唯一邮箱
     * @param purpose 注册、登录或重置用途
     * @param codeHmac 含挑战ID、邮箱和用途的验证码HMAC
     * @param deliveryCiphertext 待发送验证码AEAD密文封装，发送后清除
     * @param expiresAt 有时间适用性的事实到期时间
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static EmailChallengeAggregate pendingDelivery(
            String publicId,
            String emailNormalized,
            String purpose,
            byte[] codeHmac,
            byte[] deliveryCiphertext,
            java.time.LocalDateTime expiresAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建待投递验证码，摘要与加密载荷分开保存，具体变更委托实体。
        return new EmailChallengeAggregate(
                EmailChallengeEntity.pendingDelivery(
                        publicId,
                        emailNormalized,
                        purpose,
                        codeHmac,
                        deliveryCiphertext,
                        expiresAt,
                        createdAt,
                        updatedAt));
    }
}
