package com.hellotravel.infrastructure.knowledge.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.exception.InfrastructureErrorCode;
import com.hellotravel.infrastructure.exception.InfrastructureException;
import com.hellotravel.infrastructure.knowledge.mysql.mapper.KnowledgeDocumentMapper;
import com.hellotravel.infrastructure.knowledge.mysql.pojo.KnowledgeDocumentPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_knowledge_document仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class KnowledgeDocumentRepositoryImpl
        extends TravelBaseRepository<KnowledgeDocumentMapper, KnowledgeDocumentPO>
        implements KnowledgeDocumentRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public KnowledgeDocumentAggregate findById(Long id) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        KnowledgeDocumentPO po = getById(id);
        // 2. 显式处理不存在的记录，并恢复聚合快照。
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<KnowledgeDocumentAggregate> query(QueryValue queryValue) {
        // 1. 按字段白名单组装参数绑定条件，禁止任意列或拼接SQL。
        QueryWrapper<KnowledgeDocumentPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "title",
                                "original_filename",
                                "mime_type",
                                "storage_key",
                                "byte_size",
                                "content_sha256",
                                "extracted_text",
                                "source_url",
                                "source_accessed_at",
                                "policy_effective_at",
                                "status",
                                "index_generation",
                                "embedding_model",
                                "embedding_dimension",
                                "collection_name",
                                "error_code",
                                "deleted_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<KnowledgeDocumentPO> page = new Page<>(1, queryValue.limit(), false);
        // 2. 读取有界PO集合并恢复完整聚合，不向上暴露ORM对象。
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(KnowledgeDocumentAggregate aggregate) {
        try {
            // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
            KnowledgeDocumentPO po = toPersistence(aggregate);
            // 2. 区分新快照新增与已保存快照的版本CAS更新。
            if (po.getId() == null) {
                return super.save(po);
            }
            // 3. 映射本段快照字段，业务状态规则不放入PO赋值。
            po.setUpdatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            po.setVersion(aggregate.entity().version() + 1);
            // 4. 按内部主键及原版本执行CAS更新，零匹配由上层处理为冲突。
            return super.update(
                    po,
                    new QueryWrapper<KnowledgeDocumentPO>()
                            .eq("id", po.getId())
                            .eq("version", aggregate.entity().version()));
        } catch (org.springframework.dao.TransientDataAccessException
                | org.springframework.dao.DataIntegrityViolationException exception) {
            throw new InfrastructureException(InfrastructureErrorCode.CONFLICT);
        } catch (org.springframework.dao.DataAccessResourceFailureException exception) {
            throw new InfrastructureException(InfrastructureErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 物理清理指定记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean remove(Long id) {
        return super.removeById(id);
    }

    private KnowledgeDocumentAggregate restore(KnowledgeDocumentPO po) {
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

    private
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
