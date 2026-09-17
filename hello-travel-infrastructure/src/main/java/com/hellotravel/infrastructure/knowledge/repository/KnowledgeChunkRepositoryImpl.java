package com.hellotravel.infrastructure.knowledge.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeChunkEntity;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.knowledge.mysql.mapper.KnowledgeChunkMapper;
import com.hellotravel.infrastructure.knowledge.mysql.pojo.KnowledgeChunkPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_knowledge_chunk仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class KnowledgeChunkRepositoryImpl
        extends TravelBaseRepository<KnowledgeChunkMapper, KnowledgeChunkPO>
        implements KnowledgeChunkRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public KnowledgeChunkAggregate findById(Long id) {
        KnowledgeChunkPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<KnowledgeChunkAggregate> query(QueryValue queryValue) {
        QueryWrapper<KnowledgeChunkPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "document_id",
                                "index_generation",
                                "chunk_no",
                                "content",
                                "content_sha256",
                                "estimated_tokens",
                                "vector_key",
                                "status",
                                "deleted_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<KnowledgeChunkPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(KnowledgeChunkAggregate aggregate) {
        try {
            KnowledgeChunkPO po = new KnowledgeChunkPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setDocumentId(aggregate.entity().documentId());
            po.setIndexGeneration(aggregate.entity().indexGeneration());
            po.setChunkNo(aggregate.entity().chunkNo());
            po.setContent(aggregate.entity().content());
            po.setContentSha256(aggregate.entity().contentSha256());
            po.setEstimatedTokens(aggregate.entity().estimatedTokens());
            po.setVectorKey(aggregate.entity().vectorKey());
            po.setStatus(aggregate.entity().status());
            po.setDeletedAt(aggregate.entity().deletedAt());
            po.setCreatedAt(aggregate.entity().createdAt());
            po.setUpdatedAt(aggregate.entity().updatedAt());
            po.setVersion(aggregate.entity().version());
            if (po.getId() == null) {
                return super.save(po);
            }
            po.setUpdatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            po.setVersion(aggregate.entity().version() + 1);
            return super.update(
                    po,
                    new QueryWrapper<KnowledgeChunkPO>()
                            .eq("id", po.getId())
                            .eq("version", aggregate.entity().version()));
        } catch (org.springframework.dao.TransientDataAccessException
                | org.springframework.dao.DataIntegrityViolationException exception) {
            throw new com.hellotravel.infrastructure.exception.InfrastructureException(
                    com.hellotravel.infrastructure.exception.InfrastructureErrorCode.CONFLICT);
        } catch (org.springframework.dao.DataAccessResourceFailureException exception) {
            throw new com.hellotravel.infrastructure.exception.InfrastructureException(
                    com.hellotravel.infrastructure.exception.InfrastructureErrorCode.UNAVAILABLE);
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

    private KnowledgeChunkAggregate restore(KnowledgeChunkPO po) {
        return new KnowledgeChunkAggregate(
                new KnowledgeChunkEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getDocumentId(),
                        po.getIndexGeneration(),
                        po.getChunkNo(),
                        po.getContent(),
                        po.getContentSha256(),
                        po.getEstimatedTokens(),
                        po.getVectorKey(),
                        po.getStatus(),
                        po.getDeletedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
