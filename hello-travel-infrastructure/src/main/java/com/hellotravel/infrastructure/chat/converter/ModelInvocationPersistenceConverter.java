package com.hellotravel.infrastructure.chat.converter;

import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.infrastructure.chat.mysql.pojo.ModelInvocationPO;

import org.springframework.stereotype.Component;

/**
 * 持久化PO与完整领域快照双向映射；不执行IO或修改业务状态。
 *
 * @author AIGenerator
 */
@Component()
public final class ModelInvocationPersistenceConverter {

    /**
     * 从完整数据库快照恢复领域聚合，不补默认业务状态。
     *
     * @param po 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public ModelInvocationAggregate restore(ModelInvocationPO po) {
        // 1. 逐字段恢复持久化事实，领域初始化默认值不覆盖数据库事实。
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

    /**
     * 整体映射聚合为数据库快照，版本CAS由仓储承担。
     *
     * @param aggregate 完整源快照
     * @return 明确用途的映射结果
     * @author AIGenerator
     */
    public
    /**
     * 将完整聚合快照转换为本仓储PO，映射不参与业务状态决策。
     *
     * @param aggregate 待保存聚合
     * @return 数据库存储快照
     * @author AIGenerator
     */
    ModelInvocationPO toPersistence(ModelInvocationAggregate aggregate) {
        // 1. 转换完整聚合为本仓储PO，映射与状态决策分开。
        ModelInvocationPO po = new ModelInvocationPO();
        // 2. 映射本段快照字段，业务状态规则不放入PO赋值。
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
        // 3. 返回完整存储快照，由保存步骤决定新增或版本CAS更新。
        return po;
    }
}
