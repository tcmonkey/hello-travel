package com.hellotravel.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.infrastructure.TravelBaseRepository;
import com.hellotravel.infrastructure.chat.mysql.mapper.ModelInvocationMapper;
import com.hellotravel.infrastructure.chat.mysql.pojo.ModelInvocationPO;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * ht_model_invocation仓储，标准安全条件构造器与显式版本CAS，不使用自定义SQL。
 *
 * @author AIGenerator
 */
@Repository
public class ModelInvocationRepositoryImpl
        extends TravelBaseRepository<ModelInvocationMapper, ModelInvocationPO>
        implements ModelInvocationRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public ModelInvocationAggregate findById(Long id) {
        ModelInvocationPO po = getById(id);
        return po == null ? null : restore(po);
    }

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    public List<ModelInvocationAggregate> query(QueryValue queryValue) {
        QueryWrapper<ModelInvocationPO> wrapper =
                conditions(
                        queryValue,
                        Set.of(
                                "id",
                                "public_id",
                                "user_id",
                                "conversation_id",
                                "run_id",
                                "stage",
                                "run_attempt_no",
                                "attempt_no",
                                "model_name",
                                "prompt_revision",
                                "estimated_input_tokens",
                                "estimator_version",
                                "actual_input_tokens",
                                "actual_output_tokens",
                                "latency_ms",
                                "provider_request_id",
                                "status",
                                "error_code",
                                "completed_at",
                                "created_at",
                                "updated_at",
                                "version"));
        Page<ModelInvocationPO> page = new Page<>(1, queryValue.limit(), false);
        return page(page, wrapper).getRecords().stream().map(this::restore).toList();
    }

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean save(ModelInvocationAggregate aggregate) {
        try {
            ModelInvocationPO po = new ModelInvocationPO();
            po.setId(aggregate.entity().id());
            po.setPublicId(aggregate.entity().publicId());
            po.setUserId(aggregate.entity().userId());
            po.setConversationId(aggregate.entity().conversationId());
            po.setRunId(aggregate.entity().runId());
            po.setStage(aggregate.entity().stage());
            po.setRunAttemptNo(aggregate.entity().runAttemptNo());
            po.setAttemptNo(aggregate.entity().attemptNo());
            po.setModelName(aggregate.entity().modelName());
            po.setPromptRevision(aggregate.entity().promptRevision());
            po.setEstimatedInputTokens(aggregate.entity().estimatedInputTokens());
            po.setEstimatorVersion(aggregate.entity().estimatorVersion());
            po.setActualInputTokens(aggregate.entity().actualInputTokens());
            po.setActualOutputTokens(aggregate.entity().actualOutputTokens());
            po.setLatencyMs(aggregate.entity().latencyMs());
            po.setProviderRequestId(aggregate.entity().providerRequestId());
            po.setStatus(aggregate.entity().status());
            po.setErrorCode(aggregate.entity().errorCode());
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
                    new QueryWrapper<ModelInvocationPO>()
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

    private ModelInvocationAggregate restore(ModelInvocationPO po) {
        return new ModelInvocationAggregate(
                new ModelInvocationEntity(
                        po.getId(),
                        po.getPublicId(),
                        po.getUserId(),
                        po.getConversationId(),
                        po.getRunId(),
                        po.getStage(),
                        po.getRunAttemptNo(),
                        po.getAttemptNo(),
                        po.getModelName(),
                        po.getPromptRevision(),
                        po.getEstimatedInputTokens(),
                        po.getEstimatorVersion(),
                        po.getActualInputTokens(),
                        po.getActualOutputTokens(),
                        po.getLatencyMs(),
                        po.getProviderRequestId(),
                        po.getStatus(),
                        po.getErrorCode(),
                        po.getCompletedAt(),
                        po.getCreatedAt(),
                        po.getUpdatedAt(),
                        po.getVersion()));
    }
}
