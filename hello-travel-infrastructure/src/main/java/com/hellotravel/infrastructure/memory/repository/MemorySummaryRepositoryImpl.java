package com.hellotravel.infrastructure.memory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.model.entity.MemorySummaryEntity;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.memory.mysql.mapper.MemorySummaryMapper;
import com.hellotravel.infrastructure.memory.mysql.pojo.MemorySummaryPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_memory_summary仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class MemorySummaryRepositoryImpl
        extends TravelBaseRepository<MemorySummaryMapper, MemorySummaryPO>
        implements MemorySummaryRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public MemorySummaryAggregate findById(Long id) {
        MemorySummaryPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<MemorySummaryAggregate> query(QueryValue queryValue) {
        QueryWrapper<MemorySummaryPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "conversation_id",
                                "memory_epoch",
                                "covered_from_seq",
                                "covered_through_seq",
                                "structured_content",
                                "estimated_tokens",
                                "estimator_version",
                                "model_name",
                                "prompt_revision",
                                "status",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<MemorySummaryPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(MemorySummaryAggregate aggregate) {
        try {
            MemorySummaryPO po = new MemorySummaryPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setConversationId(aggregate.entity().conversationId());
            po.setMemoryEpoch(aggregate.entity().memoryEpoch());
            po.setCoveredFromSeq(aggregate.entity().coveredFromSeq());
            po.setCoveredThroughSeq(aggregate.entity().coveredThroughSeq());
            po.setStructuredContent(aggregate.entity().structuredContent());
            po.setEstimatedTokens(aggregate.entity().estimatedTokens());
            po.setEstimatorVersion(aggregate.entity().estimatorVersion());
            po.setModelName(aggregate.entity().modelName());
            po.setPromptRevision(aggregate.entity().promptRevision());
            po.setStatus(aggregate.entity().status());
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
                    new QueryWrapper<MemorySummaryPO>()
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

    private MemorySummaryAggregate restore(MemorySummaryPO po) {
        return new MemorySummaryAggregate(
                new MemorySummaryEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getMemoryEpoch(),
                        po.getCoveredFromSeq(),
                        po.getCoveredThroughSeq(),
                        po.getStructuredContent(),
                        po.getEstimatedTokens(),
                        po.getEstimatorVersion(),
                        po.getModelName(),
                        po.getPromptRevision(),
                        po.getStatus(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
