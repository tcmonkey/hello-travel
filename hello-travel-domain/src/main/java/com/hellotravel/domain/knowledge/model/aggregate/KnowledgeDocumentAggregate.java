package com.hellotravel.domain.knowledge.model.aggregate;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.knowledge.model.value.EmbeddingProfileValue;

/**
 * 场景聚合容器，只持有实体。
 *
 * @param entity 实体快照
 * @author AIGenerator
 */
public record KnowledgeDocumentAggregate(KnowledgeDocumentEntity entity) {

    /**
     * 校验聚合输入完整性，防止空实体进入业务保存。
     *
     * @author AIGenerator
     */
    public void assertComplete() {
        // 1. 拒绝空实体容器，完整聚合才可以进入保存流程。
        if (entity == null) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 提供仓储加载所需的标识；标量状态仍只由实体持有。
     *
     * @return 已保存标识或新增时空值
     * @author AIGenerator
     */
    public Long idForPersistence() {
        return entity.id();
    }

    /**
     * 委托实体核对已持久化聚合的更新约束，不在领域服务展开实体属性。
     *
     * @param stored 已恢复的聚合
     * @author AIGenerator
     */
    public void assertWritableAgainst(KnowledgeDocumentAggregate stored) {
        // 1. 更新目标不存在时返回NOT_FOUND，不将修改请求悄悄转成新增。
        if (stored == null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        // 2. 委托实体核对版本、归属与状态不变量。
        entity.assertUpdateAgainst(stored.entity());
    }

    /**
     * 更新知识文档状态并保留当前代次及归属；聚合委托实体，不重复存储标量状态。
     *
     * @param next next业务参数
     * @param text text业务参数
     * @param error error业务参数
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeDocumentAggregate transition(String next, String text, String error) {
        // 1. 更新知识文档状态并保留当前代次及归属，具体变更委托实体。
        return new KnowledgeDocumentAggregate(entity.transition(next, text, error));
    }

    /**
     * 提高知识索引代次，旧向量不得继续参与检索；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeDocumentAggregate reindex() {
        // 1. 提高知识索引代次，旧向量不得继续参与检索，具体变更委托实体。
        return new KnowledgeDocumentAggregate(entity.reindex());
    }

    /**
     * 清空消息正文与引用并记录删除时间；聚合委托实体，不重复存储标量状态。
     *
     * @return 保持归属与版本的业务快照
     * @author AIGenerator
     */
    public KnowledgeDocumentAggregate erase() {
        // 1. 清空消息正文与引用并记录删除时间，具体变更委托实体。
        return new KnowledgeDocumentAggregate(entity.erase());
    }

    /**
     * 创建已解析的知识文档，索引就绪需后续任务确认；聚合委托实体，不重复存储标量状态。
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
    public static KnowledgeDocumentAggregate received(
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
        // 1. 创建已解析的知识文档，索引就绪需后续任务确认，具体变更委托实体。
        return new KnowledgeDocumentAggregate(
                KnowledgeDocumentEntity.received(
                        userId,
                        title,
                        originalFilename,
                        mimeType,
                        storageKey,
                        byteSize,
                        contentSha256,
                        extractedText,
                        sourceUrl,
                        profile,
                        createdAt,
                        updatedAt));
    }
}
