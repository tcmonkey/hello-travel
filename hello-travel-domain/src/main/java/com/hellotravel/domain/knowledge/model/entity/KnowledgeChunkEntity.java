package com.hellotravel.domain.knowledge.model.entity;

import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

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
     * 防御性复制敏感字节字段，外部数组修改不能影响实体快照。
     *
     * @author AIGenerator
     */
    public KnowledgeChunkEntity {
        // 1. 复制输入摘要或加密载荷，外部数组修改不能改变实体快照。
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
        // 1. 交付摘要的独立副本，调用者修改返回数组不会改变实体快照。
        return contentSha256 == null ? null : contentSha256.clone();
    }

    /**
     * 创建正文与向量键一致的待索引分块，初始代次由任务提供。
     *
     * @param job 当前索引任务
     * @param number 文档分块顺序
     * @param body 原始正文
     * @param hash 正文摘要
     * @param tokens 保守token估算
     * @param time 创建时间
     * @return 待索引分块
     * @author AIGenerator
     */
    public static KnowledgeChunkEntity pending(
            IndexJobEntity job,
            int number,
            String body,
            byte[] hash,
            int tokens,
            java.time.LocalDateTime time) {
        // 1. 准备当前操作的存储或签名标识。
        String key = Ids.next();
        // 2. 创建待执行任务并固定初始尝试与租约，返回不可变快照。
        return new KnowledgeChunkEntity(
                null,
                key,
                job.userId(),
                job.documentId(),
                job.indexGeneration(),
                number,
                body,
                hash,
                tokens,
                key,
                "PENDING",
                null,
                time,
                time,
                0L);
    }

    /**
     * 完成当前分块索引，保持正文、归属、代次与数据库版本不变。
     *
     * @return 可检索的分块快照
     * @author AIGenerator
     */
    public KnowledgeChunkEntity ready() {
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
        if (deletedAt != null || !"PENDING".equals(status)) {
            throw new IllegalStateException("chunk not pending");
        }
        // 2. 完成分块索引，保持正文摘要与向量键不变，返回不可变快照。
        return new KnowledgeChunkEntity(
                id,
                publicId,
                userId,
                documentId,
                indexGeneration,
                chunkNo,
                content,
                contentSha256,
                estimatedTokens,
                vectorKey,
                "READY",
                deletedAt,
                createdAt,
                updatedAt,
                version);
    }

    /**
     * 核对快照更新的不变量，保持版本、归属与删除状态一致。
     *
     * @param prior 已持久化的实体快照
     * @author AIGenerator
     */
    public void assertUpdateAgainst(KnowledgeChunkEntity prior) {
        // 1. 核对数据库版本未发生并发变化，不满足时拒绝本次更新。
        if (!prior.version().equals(this.version())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        // 2. 核对账号归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.userId(), this.userId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 核对知识文档归属不变，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.documentId(), this.documentId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 4. 核对公开标识不被改写，不满足时拒绝本次更新。
        if (!java.util.Objects.equals(prior.publicId(), this.publicId())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 5. 核对已删除快照不能恢复为未删除，不满足时拒绝本次更新。
        if (prior.deletedAt() != null && this.deletedAt() == null) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }

    /**
     * 清除过期知识正文并标记删除，固定状态由实体封装。
     *
     * @param deletedAt 逻辑删除时间，后续不得参与记忆或检索
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeChunkEntity deleted(java.time.LocalDateTime deletedAt) {
        // 1. 清除过期知识正文并标记删除，返回不可变快照。
        return new KnowledgeChunkEntity(
                this.id(),
                this.publicId(),
                this.userId(),
                this.documentId(),
                this.indexGeneration(),
                this.chunkNo(),
                "",
                this.contentSha256(),
                this.estimatedTokens(),
                this.vectorKey(),
                "DELETED",
                deletedAt,
                this.createdAt(),
                this.updatedAt(),
                this.version());
    }
}
