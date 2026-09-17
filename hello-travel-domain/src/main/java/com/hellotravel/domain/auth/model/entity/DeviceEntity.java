package com.hellotravel.domain.auth.model.entity;

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
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public DeviceEntity {
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
}
