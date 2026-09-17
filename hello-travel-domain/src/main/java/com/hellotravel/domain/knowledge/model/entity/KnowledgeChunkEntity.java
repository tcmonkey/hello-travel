package com.hellotravel.domain.knowledge.model.entity;

/**
 * 持久化归属与状态快照；变更须经过语义方法和版本检查。
 *
 * @param id 内部主键，不直接作为前端数值ID
 * @param publicId 对外ULID字符串标识
 * @param userId 账号归属
 * @param documentId 知识文档归属
 * @param indexGeneration 匹配文档当前索引代次
 * @param chunkNo 文档内分块顺序
 * @param content 块正文，检索后在MySQL二次校验
 * @param contentSha256 块内容摘要，防过期向量对应错误正文
 * @param estimatedTokens 分块token估算
 * @param vectorKey Milvus主键，对应块public_id
 * @param status 分块索引状态
 * @param deletedAt 块删除时间
 * @param createdAt 创建时间，UTC
 * @param updatedAt 更新时间，UTC
 * @param version 乐观锁版本，更新时递增
 * @author AIGenerator
 */
public record KnowledgeChunkEntity(
        Long id,
        String publicId,
        Long userId,
        Long documentId,
        Long indexGeneration,
        Integer chunkNo,
        String content,
        byte[] contentSha256,
        Integer estimatedTokens,
        String vectorKey,
        String status,
        java.time.LocalDateTime deletedAt,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        Long version) {

    /**
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public KnowledgeChunkEntity {
        contentSha256 = contentSha256 == null ? null : contentSha256.clone();
    }

    /**
     * 实体诊断仅输出类型，避免泄漏正文与密码摘要。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "KnowledgeChunkEntity{redacted}";
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
