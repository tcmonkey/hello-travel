package com.hellotravel.infrastructure.sync.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;
import com.hellotravel.domain.sync.repository.SyncEventRepository;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.sync.mysql.mapper.SyncEventMapper;
import com.hellotravel.infrastructure.sync.mysql.pojo.SyncEventPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_sync_event仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class SyncEventRepositoryImpl extends TravelBaseRepository<SyncEventMapper, SyncEventPO>
        implements SyncEventRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public SyncEventAggregate findById(Long id) {
        SyncEventPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<SyncEventAggregate> query(QueryValue queryValue) {
        QueryWrapper<SyncEventPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "user_id",
                                "event_seq",
                                "event_type",
                                "aggregate_public_id",
                                "aggregate_version",
                                "target_session_public_id",
                                "payload_json",
                                "created_at",
                                "expires_at"));
        Page<SyncEventPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(SyncEventAggregate aggregate) {
        try {
            SyncEventPO po = new SyncEventPO();
            po.setId(aggregate.entity().id());
            po.setUserId(aggregate.entity().userId());
            po.setEventSeq(aggregate.entity().eventSeq());
            po.setEventType(aggregate.entity().eventType());
            po.setAggregatePublicId(aggregate.entity().aggregatePublicId());
            po.setAggregateVersion(aggregate.entity().aggregateVersion());
            po.setTargetSessionPublicId(aggregate.entity().targetSessionPublicId());
            po.setPayloadJson(aggregate.entity().payloadJson());
            po.setCreatedAt(aggregate.entity().createdAt());
            po.setExpiresAt(aggregate.entity().expiresAt());
            if (po.getId() == null) {
                return super.save(po);
            }
            return super.update(po, new QueryWrapper<SyncEventPO>().eq("id", po.getId()));
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

    private SyncEventAggregate restore(SyncEventPO po) {
        return new SyncEventAggregate(
                new SyncEventEntity(
                        po.getId(),
                        po.getUserId(),
                        po.getEventSeq(),
                        po.getEventType(),
                        po.getAggregatePublicId(),
                        po.getAggregateVersion(),
                        po.getTargetSessionPublicId(),
                        po.getPayloadJson(),
                        po.getCreatedAt(),
                        po.getExpiresAt()));
    }
}
