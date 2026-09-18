package com.hellotravel.infrastructure.knowledge.converter;

import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.infrastructure.knowledge.mysql.pojo.KnowledgeDocumentPO;

import org.springframework.stereotype.Component;

/**
 * 持久化PO与完整领域快照双向映射；不执行IO或修改业务状态。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeDocumentPersistenceConverter {

    /**
     * 从完整数据库快照恢复领域聚合，不补默认业务状态。
     *
     * @param po 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public KnowledgeDocumentAggregate restore(KnowledgeDocumentPO po) {
        // 1. 逐字段恢复持久化事实，领域初始化默认值不覆盖数据库事实。
        return new KnowledgeDocumentAggregate(
                new KnowledgeDocumentEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getTitle(),
                        po.getOriginalFilename(),
                        po.getMimeType(),
                        po.getStorageKey(),
                        po.getByteSize(),
                        po.getContentSha256(),
                        po.getExtractedText(),
                        po.getSourceUrl(),
                        po.getSourceAccessedAt(),
                        po.getPolicyEffectiveAt(),
                        po.getStatus(),
                        po.getIndexGeneration(),
                        po.getEmbeddingModel(),
                        po.getEmbeddingDimension(),
                        po.getCollectionName(),
                        po.getErrorCode(),
                        po.getDeletedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }

    /**
     * 整体映射聚合为数据库快照，版本CAS由仓储承担。
     *
     * @param aggregate 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public
    /**
     * 将完整聚合快照转换为本仓储PO，映射不参与业务状态决策。
     *
     * @param aggregate 待保存聚合
     * @return 数据库存储快照
     * @author AIGenerator
     */
    KnowledgeDocumentPO toPersistence(KnowledgeDocumentAggregate aggregate) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        KnowledgeDocumentPO po = new KnowledgeDocumentPO();
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
        po.setId(aggregate.entity().id());
        po.setPublicId(aggregate.entity().publicId());
        po.setUserId(aggregate.entity().userId());
        po.setTitle(aggregate.entity().title());
        po.setOriginalFilename(aggregate.entity().originalFilename());
        po.setMimeType(aggregate.entity().mimeType());
        po.setStorageKey(aggregate.entity().storageKey());
        po.setByteSize(aggregate.entity().byteSize());
        po.setContentSha256(aggregate.entity().contentSha256());
        po.setExtractedText(aggregate.entity().extractedText());
        po.setSourceUrl(aggregate.entity().sourceUrl());
        po.setSourceAccessedAt(aggregate.entity().sourceAccessedAt());
        po.setPolicyEffectiveAt(aggregate.entity().policyEffectiveAt());
        po.setStatus(aggregate.entity().status());
        po.setIndexGeneration(aggregate.entity().indexGeneration());
        po.setEmbeddingModel(aggregate.entity().embeddingModel());
        po.setEmbeddingDimension(aggregate.entity().embeddingDimension());
        po.setCollectionName(aggregate.entity().collectionName());
        po.setErrorCode(aggregate.entity().errorCode());
        po.setDeletedAt(aggregate.entity().deletedAt());
        po.setCreatedAt(aggregate.entity().createdAt());
        po.setUpdatedAt(aggregate.entity().updatedAt());
        po.setVersion(aggregate.entity().version());
        // 3. 返回完整存储快照，由保存步骤决定新增或版本CAS更新。
        return po;
    }
}
