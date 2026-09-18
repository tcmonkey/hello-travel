package com.hellotravel.domain.auth.model.aggregate;

import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record DeviceAggregate(DeviceEntity entity) {

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
    public void assertWritableAgainst(DeviceAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 创建当前账号的浏览器设备实例；聚合委托实体，不重复存储标量状态。
     *
     * @param userId 账号归属
     * @param hash hash业务参数
     * @param time time业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static DeviceAggregate browser(Long userId, byte[] hash, java.time.LocalDateTime time) {
        // 1. 创建当前账号的浏览器设备实例，具体变更委托实体。
        return new DeviceAggregate(DeviceEntity.browser(userId, hash, time));
    }
}
