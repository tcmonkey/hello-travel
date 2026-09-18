package com.hellotravel.domain.auth.model.aggregate;

import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record LoginSessionAggregate(LoginSessionEntity entity) {

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
    public void assertWritableAgainst(LoginSessionAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 撤销活动会话并清除访问、刷新与CSRF摘要；聚合委托实体，不重复存储标量状态。
     *
     * @param reason reason业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public LoginSessionAggregate revoke(String reason) {
        // 1. 撤销活动会话并清除访问、刷新与CSRF摘要，具体变更委托实体。
        return new LoginSessionAggregate(entity.revoke(reason));
    }

    /**
     * 轮换活动会话的随机凭据，保留原刷新有效期；聚合委托实体，不重复存储标量状态。
     *
     * @param access access业务参数
     * @param refresh refresh业务参数
     * @param csrf csrf业务参数
     * @param until until业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public LoginSessionAggregate rotate(
            byte[] access, byte[] refresh, byte[] csrf, java.time.LocalDateTime until) {
        // 1. 轮换活动会话的随机凭据，保留原刷新有效期，具体变更委托实体。
        return new LoginSessionAggregate(entity.rotate(access, refresh, csrf, until));
    }

    /**
     * 签发浏览器设备的新会话并固定认证代次及有效期；聚合委托实体，不重复存储标量状态。
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
    public static LoginSessionAggregate issue(
            UserAccountEntity user,
            DeviceEntity device,
            String publicId,
            byte[] accessHash,
            byte[] refreshHash,
            byte[] csrfHash,
            java.time.LocalDateTime time) {
        // 1. 签发浏览器设备的新会话并固定认证代次及有效期，具体变更委托实体。
        return new LoginSessionAggregate(
                LoginSessionEntity.issue(
                        user, device, publicId, accessHash, refreshHash, csrfHash, time));
    }
}
