package com.hellotravel.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.chat.mysql.mapper.ChatRunMapper;
import com.hellotravel.infrastructure.chat.mysql.pojo.ChatRunPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_chat_run仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class ChatRunRepositoryImpl extends TravelBaseRepository<ChatRunMapper, ChatRunPO>
        implements ChatRunRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public ChatRunAggregate findById(Long id) {
        ChatRunPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<ChatRunAggregate> query(QueryValue queryValue) {
        QueryWrapper<ChatRunPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "conversation_id",
                                "initiating_session_id",
                                "request_key",
                                "request_digest",
                                "user_message_id",
                                "assistant_message_id",
                                "status",
                                "attempt_count",
                                "graph_node",
                                "graph_revision",
                                "memory_epoch_at_start",
                                "state_json",
                                "context_snapshot_json",
                                "lease_owner",
                                "lease_fence",
                                "lease_until",
                                "error_code",
                                "started_at",
                                "completed_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<ChatRunPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(ChatRunAggregate aggregate) {
        try {
            ChatRunPO po = new ChatRunPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setConversationId(aggregate.entity().conversationId());
            po.setInitiatingSessionId(aggregate.entity().initiatingSessionId());
            po.setRequestKey(aggregate.entity().requestKey());
            po.setRequestDigest(aggregate.entity().requestDigest());
            po.setUserMessageId(aggregate.entity().userMessageId());
            po.setAssistantMessageId(aggregate.entity().assistantMessageId());
            po.setStatus(aggregate.entity().status());
            po.setAttemptCount(aggregate.entity().attemptCount());
            po.setGraphNode(aggregate.entity().graphNode());
            po.setGraphRevision(aggregate.entity().graphRevision());
            po.setMemoryEpochAtStart(aggregate.entity().memoryEpochAtStart());
            po.setStateJson(aggregate.entity().stateJson());
            po.setContextSnapshotJson(aggregate.entity().contextSnapshotJson());
            po.setLeaseOwner(aggregate.entity().leaseOwner());
            po.setLeaseFence(aggregate.entity().leaseFence());
            po.setLeaseUntil(aggregate.entity().leaseUntil());
            po.setErrorCode(aggregate.entity().errorCode());
            po.setStartedAt(aggregate.entity().startedAt());
            po.setCompletedAt(aggregate.entity().completedAt());
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
                    new QueryWrapper<ChatRunPO>()
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

    private ChatRunAggregate restore(ChatRunPO po) {
        return new ChatRunAggregate(
                new ChatRunEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getInitiatingSessionId(),
                        po.getRequestKey(),
                        po.getRequestDigest(),
                        po.getUserMessageId(),
                        po.getAssistantMessageId(),
                        po.getStatus(),
                        po.getAttemptCount(),
                        po.getGraphNode(),
                        po.getGraphRevision(),
                        po.getMemoryEpochAtStart(),
                        po.getStateJson(),
                        po.getContextSnapshotJson(),
                        po.getLeaseOwner(),
                        po.getLeaseFence(),
                        po.getLeaseUntil(),
                        po.getErrorCode(),
                        po.getStartedAt(),
                        po.getCompletedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
