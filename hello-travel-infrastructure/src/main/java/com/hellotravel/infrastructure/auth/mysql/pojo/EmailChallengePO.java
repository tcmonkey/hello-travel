package com.hellotravel.infrastructure.auth.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_email_challenge映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_email_challenge")
public class EmailChallengePO {

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
     * 目标邮箱，登录码签发时可无账号。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String emailNormalized;

    /**
     * 登录或密码设置/重置用途。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String purpose;

    /**
     * 含挑战ID、邮箱和用途的验证码HMAC。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private byte[] codeHmac;

    /**
     * 验证码校验密钥版本标识，无密钥值。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String codeKeyVersion;

    /**
     * 待发送验证码AEAD密文封装，发送后清除。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private byte[] deliveryCiphertext;

    /**
     * 发送密钥版本标识，独立于校验密钥。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String deliveryKeyVersion;

    /**
     * 投递及消费状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /**
     * 失败验证次数。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer attempts;

    /**
     * 允许的失败次数上限。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxAttempts;

    /**
     * 验证码到期时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime expiresAt;

    /**
     * 成功消费时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime consumedAt;

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
