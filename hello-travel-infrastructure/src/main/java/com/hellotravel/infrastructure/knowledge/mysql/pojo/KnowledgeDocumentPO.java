package com.hellotravel.infrastructure.knowledge.mysql.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * MySQL表ht_knowledge_document映射，不跨基础设施边界。
 *
 * @author AIGenerator
 */
@Data
@TableName("ht_knowledge_document")
public class KnowledgeDocumentPO {

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
     * 账号归属，一期无隐式全站共享。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long userId;

    /**
     * 资料标题。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;

    /**
     * 原文件名仅用于展示，不作磁盘路径。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String originalFilename;

    /**
     * 服务端检测的内容类型。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String mimeType;

    /**
     * 服务端生成的相对存储键，禁止任意路径。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String storageKey;

    /**
     * 原文件大小，应用配置上限。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long byteSize;

    /**
     * 原始文件SHA256校验及重复导入识别。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private byte[] contentSha256;

    /**
     * 提取的明文，页面分段加载；应用有提取上限。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String extractedText;

    /**
     * 官方来源链接或用户提供的来源，仅作引用。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sourceUrl;

    /**
     * 资料访问/核验时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime sourceAccessedAt;

    /**
     * 来源明确提供时记录政策生效时间。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private java.time.LocalDateTime policyEffectiveAt;

    /**
     * 上传、解析、索引及删除状态。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String status;

    /**
     * 重试/重建的索引代次，防旧任务覆盖。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long indexGeneration;

    /**
     * 嵌入模型名称。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String embeddingModel;

    /**
     * 固定向量维度，变化需新集合。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer embeddingDimension;

    /**
     * hello_travel专用Milvus集合名。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String collectionName;

    /**
     * 可展示内部错误码。
     *
     * @author AIGenerator
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorCode;

    /**
     * 逻辑删除时间，后续RAG立即排除。
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
