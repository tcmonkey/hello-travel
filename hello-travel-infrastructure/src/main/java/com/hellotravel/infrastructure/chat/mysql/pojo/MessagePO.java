package com.hellotravel.infrastructure.chat.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_message映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_message")
public class MessagePO {

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
     * 账号归属。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    /**
     * 对话归属。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long conversationId;

    /**
     * 对话内稳定顺序，删除不复用。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long messageSeq;

    /**
     * 用户或助手；系统规则不混作用户消息。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String role;

    /**
     * 提交、生成、完成或失败等状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /**
     * 消息正文；流式草稿可定期保存，受应用大小限制。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String content;

    /**
     * 已核验的引用片段ID、来源、日期和版本。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String citationsJson;

    /**
     * 逻辑删除时间，后续不得参与记忆或检索。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime deletedAt;

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
