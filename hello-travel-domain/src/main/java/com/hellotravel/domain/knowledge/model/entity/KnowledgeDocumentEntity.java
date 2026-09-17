package com.hellotravel.domain.knowledge.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属，一期无隐式全站共享
 * @param title 资料标题
 * @param originalFilename 原文件名仅用于展示，不作磁盘路径
 * @param mimeType 服务端检测的内容类型
 * @param storageKey 服务端生成的相对存储键，禁止任意路径
 * @param byteSize 原文件大小，应用配置上限
 * @param contentSha256 原始文件SHA256校验及重复导入识别
 * @param extractedText 提取的明文，页面分段加载；应用有提取上限
 * @param sourceUrl 官方来源链接或用户提供的来源，仅作引用
 * @param sourceAccessedAt 资料访问/核验时间
 * @param policyEffectiveAt 来源明确提供时记录政策生效时间
 * @param status 上传、解析、索引及删除状态
 * @param indexGeneration 重试/重建的索引代次，防旧任务覆盖
 * @param embeddingModel 嵌入模型名称
 * @param embeddingDimension 固定向量维度，变化需新集合
 * @param collectionName hello_travel专用Milvus集合名
 * @param errorCode 可展示内部错误码
 * @param deletedAt 逻辑删除时间，后续RAG立即排除
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record KnowledgeDocumentEntity(
        Long id,
        String publicId,
        Long userId,
        String title,
        String originalFilename,
        String mimeType,
        String storageKey,
        Long byteSize,
        byte[] contentSha256,
        String extractedText,
        String sourceUrl,
        java.time.LocalDateTime sourceAccessedAt,
        java.time.LocalDateTime policyEffectiveAt,
        String status,
        Long indexGeneration,
        String embeddingModel,
        Integer embeddingDimension,
        String collectionName,
        String errorCode,
        java.time.LocalDateTime deletedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public KnowledgeDocumentEntity {
        contentSha256 = contentSha256 == null ? null : contentSha256.clone();
    }

    /**
     * 转换资料处理状态并保留明确错误分类。
     *
     * @author AIGenerator
     * @param next 受控next参数
     * @param text 有界文本内容
     * @param error 待分类的失败
     * @return 当前操作的业务结果
     */
    public KnowledgeDocumentEntity transition(String next, String text, String error) {
        if (deletedAt != null && !java.util.Set.of("DELETING", "DELETED").contains(next)) {
            throw new IllegalStateException("deleted document");
        }
        return new KnowledgeDocumentEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.title(),
                this.originalFilename(),
                this.mimeType(),
                this.storageKey(),
                this.byteSize(),
                this.contentSha256(),
                text,
                this.sourceUrl(),
                this.sourceAccessedAt(),
                this.policyEffectiveAt(),
                next,
                this.indexGeneration(),
                this.embeddingModel(),
                this.embeddingDimension(),
                this.collectionName(),
                error,
                this.deletedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 为失败资料创建新的索引代次。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public KnowledgeDocumentEntity reindex() {
        if (!"FAILED".equals(status) || deletedAt != null) {
            throw new IllegalStateException("not retryable");
        }
        return new KnowledgeDocumentEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.title(),
                this.originalFilename(),
                this.mimeType(),
                this.storageKey(),
                this.byteSize(),
                this.contentSha256(),
                this.extractedText(),
                this.sourceUrl(),
                this.sourceAccessedAt(),
                this.policyEffectiveAt(),
                "RECEIVED",
                Math.addExact(indexGeneration, 1),
                this.embeddingModel(),
                this.embeddingDimension(),
                this.collectionName(),
                null,
                this.deletedAt(),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 清除消息正文和引用并记录删除时间。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public KnowledgeDocumentEntity erase() {
        return new KnowledgeDocumentEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.title(),
                this.originalFilename(),
                this.mimeType(),
                this.storageKey(),
                this.byteSize(),
                this.contentSha256(),
                this.extractedText(),
                this.sourceUrl(),
                this.sourceAccessedAt(),
                this.policyEffectiveAt(),
                "DELETING",
                this.indexGeneration(),
                this.embeddingModel(),
                this.embeddingDimension(),
                this.collectionName(),
                this.errorCode(),
                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "KnowledgeDocumentEntity{redacted}";
    }

    /**
     * 处理contentSha256对应的受控业务操作。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public byte[] contentSha256() {
        return contentSha256 == null ? null : contentSha256.clone();
    }
}
