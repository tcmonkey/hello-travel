package com.hellotravel.infrastructure.sync.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_sync_event映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_sync_event")
public class SyncEventPO {

    /**
     * 内部主键，不作为客户端补齐游标。
     *
     * @author AIGenerator
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标账号，其他账号不得订阅。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    /**
     * 该用户的连续提交序号；来自账号sync_seq。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long eventSeq;

    /**
     * 消息、会话、用量、资料或会话撤销事件类型。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String eventType;

    /**
     * 事件目标对外ID。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aggregatePublicId;

    /**
     * 目标实体版本，避免重放覆盖新状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long aggregateVersion;

    /**
     * 定向踢登录会话事件的目标。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String targetSessionPublicId;

    /**
     * 有界ID/状态/版本载荷，不含令牌、验证码或完整私密正文。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String payloadJson;

    /**
     * 与业务事务一起提交的UTC时间。
     *
     * @author AIGenerator
     */
    private java.time.LocalDateTime createdAt;

    /**
     * 补齐事件保留截止；过期需要重新拉完整快照。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime expiresAt;
}
