package com.hellotravel.domain.auth.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param deviceKeyHash 服务端设备Cookie随机标识的哈希，不是物理指纹
 * @param deviceLabel 可读设备名称，安全文本
 * @param lastSeenAt 最近活跃时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record DeviceEntity(
        Long id,
        String publicId,
        Long userId,
        byte[] deviceKeyHash,
        String deviceLabel,
        java.time.LocalDateTime lastSeenAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public DeviceEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
        deviceKeyHash = deviceKeyHash == null ? null : deviceKeyHash.clone();
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "DeviceEntity{redacted}";
    }

    /**
     * 处理deviceKeyHash对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] deviceKeyHash() {
        return deviceKeyHash == null ? null : deviceKeyHash.clone();
    }

    /**
     * 为用户注册浏览器设备实例，设备键采用摘要且不伪造物理指纹。
     *
     * @param userId 当前账号
     * @param hash 设备键摘要
     * @param time 当前UTC时间
     * @return 新设备快照
     * @author AIGenerator
     */
    public static DeviceEntity browser(Long userId, byte[] hash, java.time.LocalDateTime time) {
        // 1. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        String publicId = Ids.next();
        // 2. 创建当前账号的浏览器设备实例，返回不可变快照。
        return new DeviceEntity(null, publicId, userId, hash, "浏览器", time, time, time, 0L);
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(DeviceEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }
}
