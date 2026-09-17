package com.hellotravel.infrastructure.memory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.entity.MemoryFactEntity;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.memory.mysql.mapper.MemoryFactMapper;
import com.hellotravel.infrastructure.memory.mysql.pojo.MemoryFactPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_memory_fact仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class MemoryFactRepositoryImpl extends TravelBaseRepository<MemoryFactMapper, MemoryFactPO>
        implements MemoryFactRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public MemoryFactAggregate findById(Long id) {
        MemoryFactPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<MemoryFactAggregate> query(QueryValue queryValue) {
        QueryWrapper<MemoryFactPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "conversation_id",
                                "memory_epoch",
                                "fact_key",
                                "category",
                                "content",
                                "evidence_type",
                                "source_count",
                                "status",
                                "expires_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<MemoryFactPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(MemoryFactAggregate aggregate) {
        try {
            MemoryFactPO po = new MemoryFactPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setConversationId(aggregate.entity().conversationId());
            po.setMemoryEpoch(aggregate.entity().memoryEpoch());
            po.setFactKey(aggregate.entity().factKey());
            po.setCategory(aggregate.entity().category());
            po.setContent(aggregate.entity().content());
            po.setEvidenceType(aggregate.entity().evidenceType());
            po.setSourceCount(aggregate.entity().sourceCount());
            po.setStatus(aggregate.entity().status());
            po.setExpiresAt(aggregate.entity().expiresAt());
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
                    new QueryWrapper<MemoryFactPO>()
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

    private MemoryFactAggregate restore(MemoryFactPO po) {
        return new MemoryFactAggregate(
                new MemoryFactEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getMemoryEpoch(),
                        po.getFactKey(),
                        po.getCategory(),
                        po.getContent(),
                        po.getEvidenceType(),
                        po.getSourceCount(),
                        po.getStatus(),
                        po.getExpiresAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
