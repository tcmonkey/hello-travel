package com.hellotravel.infrastructure.auth.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_user_account映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_user_account")
public class UserAccountPO {

    /**
     * 内部主键，不直接作为前端数值ID。
     *
     * @author AIGenerator
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 对外ULID字符串标识。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String publicId;

    /**
     * 应用规范化后的唯一邮箱。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String emailNormalized;

    /**
     * 带算法和参数的加盐密码哈希，不存明文。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String passwordHash;

    /**
     * 邮箱验证成功时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime emailVerifiedAt;

    /**
     * 账号状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /**
     * 全设备撤销版本，重置密码时递增。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long authEpoch;

    /**
     * 按用户分配并提交的持久同步序号。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long syncSeq;

    /**
     * 创建时间，UTC。
     *
     * @author AIGenerator
     */
    private java.time.LocalDateTime createdAt;

    /**
     * 更新时间，UTC。
     *
     * @author AIGenerator
     */
    private java.time.LocalDateTime updatedAt;

    /**
     * 乐观锁版本，更新时递增。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long version;
}
