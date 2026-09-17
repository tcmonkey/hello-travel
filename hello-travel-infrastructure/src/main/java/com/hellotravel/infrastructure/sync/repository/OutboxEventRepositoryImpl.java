package com.hellotravel.infrastructure.sync.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.entity.OutboxEventEntity;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.sync.mysql.mapper.OutboxEventMapper;
import com.hellotravel.infrastructure.sync.mysql.pojo.OutboxEventPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_outbox_event仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class OutboxEventRepositoryImpl
        extends TravelBaseRepository<OutboxEventMapper, OutboxEventPO>
        implements OutboxEventRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public OutboxEventAggregate findById(Long id) {
        OutboxEventPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<OutboxEventAggregate> query(QueryValue queryValue) {
        QueryWrapper<OutboxEventPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "event_type",
                                "dedupe_key",
                                "payload_json",
                                "status",
                                "attempt_count",
                                "max_attempts",
                                "next_attempt_at",
                                "lease_owner",
                                "lease_fence",
                                "lease_until",
                                "delivered_at",
                                "error_code",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<OutboxEventPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(OutboxEventAggregate aggregate) {
        try {
            OutboxEventPO po = new OutboxEventPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setEventType(aggregate.entity().eventType());
            po.setDedupeKey(aggregate.entity().dedupeKey());
            po.setPayloadJson(aggregate.entity().payloadJson());
            po.setStatus(aggregate.entity().status());
            po.setAttemptCount(aggregate.entity().attemptCount());
            po.setMaxAttempts(aggregate.entity().maxAttempts());
            po.setNextAttemptAt(aggregate.entity().nextAttemptAt());
            po.setLeaseOwner(aggregate.entity().leaseOwner());
            po.setLeaseFence(aggregate.entity().leaseFence());
            po.setLeaseUntil(aggregate.entity().leaseUntil());
            po.setDeliveredAt(aggregate.entity().deliveredAt());
            po.setErrorCode(aggregate.entity().errorCode());
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
                    new QueryWrapper<OutboxEventPO>()
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

    private OutboxEventAggregate restore(OutboxEventPO po) {
        return new OutboxEventAggregate(
                new OutboxEventEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getEventType(),
                        po.getDedupeKey(),
                        po.getPayloadJson(),
                        po.getStatus(),
                        po.getAttemptCount(),
                        po.getMaxAttempts(),
                        po.getNextAttemptAt(),
                        po.getLeaseOwner(),
                        po.getLeaseFence(),
                        po.getLeaseUntil(),
                        po.getDeliveredAt(),
                        po.getErrorCode(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
