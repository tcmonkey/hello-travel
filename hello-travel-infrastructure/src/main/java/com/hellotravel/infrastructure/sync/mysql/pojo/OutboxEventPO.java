package com.hellotravel.infrastructure.sync.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_outbox_event映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_outbox_event")
public class OutboxEventPO {

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
     * 可选账号归属；首次登录验证码可尚无账号。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    /**
     * 推送、开始生成、提取记忆或验证码投递等事件。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String eventType;

    /**
     * 事件投递幂等键。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dedupeKey;

    /**
     * 只存受限任务引用，验证码密文在challenge表。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String payloadJson;

    /**
     * 派发状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /**
     * 派发尝试次数。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer attemptCount;

    /**
     * 派发次数上限，死信可诊断。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxAttempts;

    /**
     * 下次派发时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime nextAttemptAt;

    /**
     * 派发执行者。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String leaseOwner;

    /**
     * 派发租约代次。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long leaseFence;

    /**
     * 租约到期时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime leaseUntil;

    /**
     * 实际完成/幂等交付时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime deliveredAt;

    /**
     * 稳定内部错误码。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorCode;

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
