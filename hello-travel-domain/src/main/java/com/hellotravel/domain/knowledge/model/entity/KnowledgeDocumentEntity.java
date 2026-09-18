package com.hellotravel.domain.knowledge.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.value.EmbeddingProfileValue;

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
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public KnowledgeDocumentEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
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
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
        if (deletedAt != null && !java.util.Set.of("DELETING", "DELETED").contains(next)) {
            throw new IllegalStateException("deleted document");
        }
        // 2. 更新知识文档状态并保留当前代次及归属，返回不可变快照。
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
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
        if (!"FAILED".equals(status) || deletedAt != null) {
            throw new IllegalStateException("not retryable");
        }
        // 2. 提高知识索引代次，旧向量不得继续参与检索，返回不可变快照。
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
        // 1. 清空消息正文与引用并记录删除时间，返回不可变快照。
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
        // 1. 交付摘要的独立副本，调用者修改返回数组不会改变实体快照。
        return contentSha256 == null ? null : contentSha256.clone();
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(KnowledgeDocumentEntity prior) {
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
        // 4. 核对已删除快照不能恢复为未删除，不满足时拒绝本次更新。
        if (prior.deletedAt() != null && this.deletedAt() == null) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }

    /**
     * 创建已解析的知识文档，索引就绪需后续任务确认，固定状态由实体封装。
     *
     * @param userId 账号归属
     * @param title 对话标题，作为纯文本展示
     * @param originalFilename 原文件名仅用于展示，不作磁盘路径
     * @param mimeType 服务端检测的内容类型
     * @param storageKey 服务端生成的相对存储键，禁止任意路径
     * @param byteSize 原文件大小，应用配置上限
     * @param contentSha256 原始文件SHA256校验及重复导入识别
     * @param extractedText 提取的明文，页面分段加载；应用有提取上限
     * @param sourceUrl 官方来源链接或用户提供的来源，仅作引用
     * @param profile 已选择的不可变索引配置
     * @param createdAt 创建时间
     * @param updatedAt 当前变更时间
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public static KnowledgeDocumentEntity received(
            Long userId,
            String title,
            String originalFilename,
            String mimeType,
            String storageKey,
            Long byteSize,
            byte[] contentSha256,
            String extractedText,
            String sourceUrl,
            EmbeddingProfileValue profile,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
        // 1. 创建已解析的知识文档，索引就绪需后续任务确认，返回不可变快照。
        return new KnowledgeDocumentEntity(
                null,
                Ids.next(),
                userId,
                title,
                originalFilename,
                mimeType,
                storageKey,
                byteSize,
                contentSha256,
                extractedText,
                sourceUrl,
                null,
                null,
                "RECEIVED",
                1L,
                profile.model(),
                profile.dimensions(),
                profile.collection(),
                null,
                null,
                createdAt,
                updatedAt,
                0L);
    }
}
