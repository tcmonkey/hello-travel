package com.hellotravel.infrastructure.memory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.entity.MemoryFactSourceEntity;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.memory.mysql.mapper.MemoryFactSourceMapper;
import com.hellotravel.infrastructure.memory.mysql.pojo.MemoryFactSourcePO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_memory_fact_source仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class MemoryFactSourceRepositoryImpl
        extends TravelBaseRepository<MemoryFactSourceMapper, MemoryFactSourcePO>
        implements MemoryFactSourceRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public MemoryFactSourceAggregate findById(Long id) {
        MemoryFactSourcePO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<MemoryFactSourceAggregate> query(QueryValue queryValue) {
        QueryWrapper<MemoryFactSourcePO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "user_id",
                                "conversation_id",
                                "fact_id",
                                "message_id",
                                "evidence_excerpt",
                                "message_version",
                                "created_at"));
        Page<MemoryFactSourcePO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(MemoryFactSourceAggregate aggregate) {
        try {
            MemoryFactSourcePO po = new MemoryFactSourcePO();
            po.setId(aggregate.entity().id());
            po.setUserId(aggregate.entity().userId());
            po.setConversationId(aggregate.entity().conversationId());
            po.setFactId(aggregate.entity().factId());
            po.setMessageId(aggregate.entity().messageId());
            po.setEvidenceExcerpt(aggregate.entity().evidenceExcerpt());
            po.setMessageVersion(aggregate.entity().messageVersion());
            po.setCreatedAt(aggregate.entity().createdAt());
            if (po.getId() == null) {
                return super.save(po);
            }
            return super.update(po, new QueryWrapper<MemoryFactSourcePO>().eq("id", po.getId()));
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

    private MemoryFactSourceAggregate restore(MemoryFactSourcePO po) {
        return new MemoryFactSourceAggregate(
                new MemoryFactSourceEntity(
                        po.getId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getFactId(),
                        po.getMessageId(),
                        po.getEvidenceExcerpt(),
                        po.getMessageVersion(),
                        po.getCreatedAt()));
    }
}
