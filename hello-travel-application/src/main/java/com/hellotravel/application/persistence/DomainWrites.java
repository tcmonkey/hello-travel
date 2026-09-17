package com.hellotravel.application.persistence;

import com.hellotravel.domain.persistence.service.TravelWriteDomainService;

import org.springframework.stereotype.Component;

/**
 * 写用例必须经过领域服务；失败抛出事务边界以保证整体回滚。
 *
 * @author AIGenerator
 */
@Component
public final class DomainWrites {

    private final TravelWriteDomainService service;

    public DomainWrites(TravelWriteDomainService service) {
        this.service = service;
    }

    /**
     * 校验UserAccount完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveUserAccount(
            com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate aggregate) {
        return required(
                        service.saveUserAccount(
                                new com.hellotravel.domain.persistence.model.param
                                        .UserAccountWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理UserAccount的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeUserAccount(Long id) {
        return required(
                        service.removeUserAccount(
                                new com.hellotravel.domain.persistence.model.param
                                        .UserAccountRemoveParam(id)))
                .saved();
    }

    /**
     * 校验Device完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveDevice(
            com.hellotravel.domain.auth.model.aggregate.DeviceAggregate aggregate) {
        return required(
                        service.saveDevice(
                                new com.hellotravel.domain.persistence.model.param.DeviceWriteParam(
                                        aggregate)))
                .saved();
    }

    /**
     * 清理Device的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeDevice(Long id) {
        return required(
                        service.removeDevice(
                                new com.hellotravel.domain.persistence.model.param
                                        .DeviceRemoveParam(id)))
                .saved();
    }

    /**
     * 校验LoginSession完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveLoginSession(
            com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate aggregate) {
        return required(
                        service.saveLoginSession(
                                new com.hellotravel.domain.persistence.model.param
                                        .LoginSessionWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理LoginSession的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeLoginSession(Long id) {
        return required(
                        service.removeLoginSession(
                                new com.hellotravel.domain.persistence.model.param
                                        .LoginSessionRemoveParam(id)))
                .saved();
    }

    /**
     * 校验EmailChallenge完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveEmailChallenge(
            com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate aggregate) {
        return required(
                        service.saveEmailChallenge(
                                new com.hellotravel.domain.persistence.model.param
                                        .EmailChallengeWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理EmailChallenge的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeEmailChallenge(Long id) {
        return required(
                        service.removeEmailChallenge(
                                new com.hellotravel.domain.persistence.model.param
                                        .EmailChallengeRemoveParam(id)))
                .saved();
    }

    /**
     * 校验Conversation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveConversation(
            com.hellotravel.domain.chat.model.aggregate.ConversationAggregate aggregate) {
        return required(
                        service.saveConversation(
                                new com.hellotravel.domain.persistence.model.param
                                        .ConversationWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理Conversation的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeConversation(Long id) {
        return required(
                        service.removeConversation(
                                new com.hellotravel.domain.persistence.model.param
                                        .ConversationRemoveParam(id)))
                .saved();
    }

    /**
     * 校验Message完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMessage(
            com.hellotravel.domain.chat.model.aggregate.MessageAggregate aggregate) {
        return required(
                        service.saveMessage(
                                new com.hellotravel.domain.persistence.model.param
                                        .MessageWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理Message的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMessage(Long id) {
        return required(
                        service.removeMessage(
                                new com.hellotravel.domain.persistence.model.param
                                        .MessageRemoveParam(id)))
                .saved();
    }

    /**
     * 校验ChatRun完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveChatRun(
            com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate aggregate) {
        return required(
                        service.saveChatRun(
                                new com.hellotravel.domain.persistence.model.param
                                        .ChatRunWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理ChatRun的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeChatRun(Long id) {
        return required(
                        service.removeChatRun(
                                new com.hellotravel.domain.persistence.model.param
                                        .ChatRunRemoveParam(id)))
                .saved();
    }

    /**
     * 校验MemorySummary完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemorySummary(
            com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate aggregate) {
        return required(
                        service.saveMemorySummary(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemorySummaryWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理MemorySummary的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemorySummary(Long id) {
        return required(
                        service.removeMemorySummary(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemorySummaryRemoveParam(id)))
                .saved();
    }

    /**
     * 校验MemoryFact完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFact(
            com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate aggregate) {
        return required(
                        service.saveMemoryFact(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemoryFactWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理MemoryFact的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemoryFact(Long id) {
        return required(
                        service.removeMemoryFact(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemoryFactRemoveParam(id)))
                .saved();
    }

    /**
     * 校验MemoryFactSource完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFactSource(
            com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate aggregate) {
        return required(
                        service.saveMemoryFactSource(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemoryFactSourceWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理MemoryFactSource的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemoryFactSource(Long id) {
        return required(
                        service.removeMemoryFactSource(
                                new com.hellotravel.domain.persistence.model.param
                                        .MemoryFactSourceRemoveParam(id)))
                .saved();
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeDocument(
            com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate aggregate) {
        return required(
                        service.saveKnowledgeDocument(
                                new com.hellotravel.domain.persistence.model.param
                                        .KnowledgeDocumentWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理KnowledgeDocument的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeKnowledgeDocument(Long id) {
        return required(
                        service.removeKnowledgeDocument(
                                new com.hellotravel.domain.persistence.model.param
                                        .KnowledgeDocumentRemoveParam(id)))
                .saved();
    }

    /**
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeChunk(
            com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate aggregate) {
        return required(
                        service.saveKnowledgeChunk(
                                new com.hellotravel.domain.persistence.model.param
                                        .KnowledgeChunkWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理KnowledgeChunk的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeKnowledgeChunk(Long id) {
        return required(
                        service.removeKnowledgeChunk(
                                new com.hellotravel.domain.persistence.model.param
                                        .KnowledgeChunkRemoveParam(id)))
                .saved();
    }

    /**
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveIndexJob(
            com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate aggregate) {
        return required(
                        service.saveIndexJob(
                                new com.hellotravel.domain.persistence.model.param
                                        .IndexJobWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理IndexJob的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeIndexJob(Long id) {
        return required(
                        service.removeIndexJob(
                                new com.hellotravel.domain.persistence.model.param
                                        .IndexJobRemoveParam(id)))
                .saved();
    }

    /**
     * 校验SyncEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveSyncEvent(
            com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate aggregate) {
        return required(
                        service.saveSyncEvent(
                                new com.hellotravel.domain.persistence.model.param
                                        .SyncEventWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理SyncEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeSyncEvent(Long id) {
        return required(
                        service.removeSyncEvent(
                                new com.hellotravel.domain.persistence.model.param
                                        .SyncEventRemoveParam(id)))
                .saved();
    }

    /**
     * 校验OutboxEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveOutboxEvent(
            com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate aggregate) {
        return required(
                        service.saveOutboxEvent(
                                new com.hellotravel.domain.persistence.model.param
                                        .OutboxEventWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理OutboxEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeOutboxEvent(Long id) {
        return required(
                        service.removeOutboxEvent(
                                new com.hellotravel.domain.persistence.model.param
                                        .OutboxEventRemoveParam(id)))
                .saved();
    }

    /**
     * 校验ModelInvocation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveModelInvocation(
            com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate aggregate) {
        return required(
                        service.saveModelInvocation(
                                new com.hellotravel.domain.persistence.model.param
                                        .ModelInvocationWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理ModelInvocation的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeModelInvocation(Long id) {
        return required(
                        service.removeModelInvocation(
                                new com.hellotravel.domain.persistence.model.param
                                        .ModelInvocationRemoveParam(id)))
                .saved();
    }

    /**
     * 校验RefreshReceipt完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveRefreshReceipt(
            com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate aggregate) {
        return required(
                        service.saveRefreshReceipt(
                                new com.hellotravel.domain.persistence.model.param
                                        .RefreshReceiptWriteParam(aggregate)))
                .saved();
    }

    /**
     * 清理RefreshReceipt的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeRefreshReceipt(Long id) {
        return required(
                        service.removeRefreshReceipt(
                                new com.hellotravel.domain.persistence.model.param
                                        .RefreshReceiptRemoveParam(id)))
                .saved();
    }

    private static com.hellotravel.model.persistence.WriteDO required(
            com.hellotravel.common.result.Result<com.hellotravel.model.persistence.WriteDO>
                    result) {
        if (!result.success()) {
            throw new com.hellotravel.domain.exception.DomainException(
                    com.hellotravel.domain.exception.DomainErrorCode.valueOf(result.code()));
        }
        return result.data();
    }
}
