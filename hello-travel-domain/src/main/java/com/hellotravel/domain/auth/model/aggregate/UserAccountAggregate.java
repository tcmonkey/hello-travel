package com.hellotravel.domain.auth.model.aggregate;

import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record UserAccountAggregate(UserAccountEntity entity) {

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
    public void assertWritableAgainst(UserAccountAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 推进同用户提交序号，与事件在同一事务提交；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public UserAccountAggregate advanceSync() {
        // 1. 推进同用户提交序号，与事件在同一事务提交，具体变更委托实体。
        return new UserAccountAggregate(entity.advanceSync());
    }

    /**
     * 更新密码摘要并提高认证代次，旧会话随之失效；聚合委托实体，不重复存储标量状态。
     *
     * @param hash hash业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public UserAccountAggregate resetPassword(String hash) {
        // 1. 更新密码摘要并提高认证代次，旧会话随之失效，具体变更委托实体。
        return new UserAccountAggregate(entity.resetPassword(hash));
    }

    /**
     * 创建邮箱已验证的新账号并固定初始认证代次；聚合委托实体，不重复存储标量状态。
     *
     * @param emailNormalized 应用规范化后的唯一邮箱
     * @param passwordHash 带算法和参数的加盐密码哈希，不存明文
     * @param emailVerifiedAt 邮箱验证成功时间
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static UserAccountAggregate registered(
            String emailNormalized,
            String passwordHash,
            java.time.LocalDateTime emailVerifiedAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建邮箱已验证的新账号并固定初始认证代次，具体变更委托实体。
        return new UserAccountAggregate(
                UserAccountEntity.registered(
                        emailNormalized, passwordHash, emailVerifiedAt, createdAt, updatedAt));
    }
}
