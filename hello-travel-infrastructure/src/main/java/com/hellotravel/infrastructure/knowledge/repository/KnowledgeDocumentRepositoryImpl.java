package com.hellotravel.infrastructure.knowledge.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.exception.InfrastructureErrorCode;
import com.hellotravel.infrastructure.exception.InfrastructureException;
import com.hellotravel.infrastructure.knowledge.converter.KnowledgeDocumentPersistenceConverter;
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

    private final KnowledgeDocumentPersistenceConverter knowledgeDocumentPersistenceConverter;

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
        return po == null ? null : knowledgeDocumentPersistenceConverter.restore(po);
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
        return page(page, wrapper).getRecords().stream()
                .map(knowledgeDocumentPersistenceConverter::restore)
                .toList();
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
            KnowledgeDocumentPO po = knowledgeDocumentPersistenceConverter.toPersistence(aggregate);
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

    public KnowledgeDocumentRepositoryImpl(
            KnowledgeDocumentPersistenceConverter knowledgeDocumentPersistenceConverter) {
        this.knowledgeDocumentPersistenceConverter = knowledgeDocumentPersistenceConverter;
    }
}
