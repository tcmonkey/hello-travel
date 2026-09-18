package com.hellotravel.application.persistence;

import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.persistence.model.param.ChatRunRemoveParam;
import com.hellotravel.domain.persistence.model.param.ChatRunWriteParam;
import com.hellotravel.domain.persistence.model.param.ConversationRemoveParam;
import com.hellotravel.domain.persistence.model.param.ConversationWriteParam;
import com.hellotravel.domain.persistence.model.param.DeviceRemoveParam;
import com.hellotravel.domain.persistence.model.param.DeviceWriteParam;
import com.hellotravel.domain.persistence.model.param.EmailChallengeRemoveParam;
import com.hellotravel.domain.persistence.model.param.EmailChallengeWriteParam;
import com.hellotravel.domain.persistence.model.param.IndexJobRemoveParam;
import com.hellotravel.domain.persistence.model.param.IndexJobWriteParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeChunkRemoveParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeChunkWriteParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeDocumentRemoveParam;
import com.hellotravel.domain.persistence.model.param.KnowledgeDocumentWriteParam;
import com.hellotravel.domain.persistence.model.param.LoginSessionRemoveParam;
import com.hellotravel.domain.persistence.model.param.LoginSessionWriteParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactSourceRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactSourceWriteParam;
import com.hellotravel.domain.persistence.model.param.MemoryFactWriteParam;
import com.hellotravel.domain.persistence.model.param.MemorySummaryRemoveParam;
import com.hellotravel.domain.persistence.model.param.MemorySummaryWriteParam;
import com.hellotravel.domain.persistence.model.param.MessageRemoveParam;
import com.hellotravel.domain.persistence.model.param.MessageWriteParam;
import com.hellotravel.domain.persistence.model.param.ModelInvocationRemoveParam;
import com.hellotravel.domain.persistence.model.param.ModelInvocationWriteParam;
import com.hellotravel.domain.persistence.model.param.OutboxEventRemoveParam;
import com.hellotravel.domain.persistence.model.param.OutboxEventWriteParam;
import com.hellotravel.domain.persistence.model.param.RefreshReceiptRemoveParam;
import com.hellotravel.domain.persistence.model.param.RefreshReceiptWriteParam;
import com.hellotravel.domain.persistence.model.param.SyncEventRemoveParam;
import com.hellotravel.domain.persistence.model.param.SyncEventWriteParam;
import com.hellotravel.domain.persistence.model.param.UserAccountRemoveParam;
import com.hellotravel.domain.persistence.model.param.UserAccountWriteParam;
import com.hellotravel.domain.persistence.service.TravelWriteDomainService;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.model.persistence.WriteDO;

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
    public Boolean saveUserAccount(UserAccountAggregate aggregate) {
        return required(service.saveUserAccount(new UserAccountWriteParam(aggregate))).saved();
    }

    /**
     * 清理UserAccount的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeUserAccount(Long id) {
        return required(service.removeUserAccount(new UserAccountRemoveParam(id))).saved();
    }

    /**
     * 校验Device完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveDevice(DeviceAggregate aggregate) {
        return required(service.saveDevice(new DeviceWriteParam(aggregate))).saved();
    }

    /**
     * 清理Device的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeDevice(Long id) {
        return required(service.removeDevice(new DeviceRemoveParam(id))).saved();
    }

    /**
     * 校验LoginSession完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveLoginSession(LoginSessionAggregate aggregate) {
        return required(service.saveLoginSession(new LoginSessionWriteParam(aggregate))).saved();
    }

    /**
     * 清理LoginSession的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeLoginSession(Long id) {
        return required(service.removeLoginSession(new LoginSessionRemoveParam(id))).saved();
    }

    /**
     * 校验EmailChallenge完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveEmailChallenge(EmailChallengeAggregate aggregate) {
        return required(service.saveEmailChallenge(new EmailChallengeWriteParam(aggregate)))
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
        return required(service.removeEmailChallenge(new EmailChallengeRemoveParam(id))).saved();
    }

    /**
     * 校验Conversation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveConversation(ConversationAggregate aggregate) {
        return required(service.saveConversation(new ConversationWriteParam(aggregate))).saved();
    }

    /**
     * 清理Conversation的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeConversation(Long id) {
        return required(service.removeConversation(new ConversationRemoveParam(id))).saved();
    }

    /**
     * 校验Message完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMessage(MessageAggregate aggregate) {
        return required(service.saveMessage(new MessageWriteParam(aggregate))).saved();
    }

    /**
     * 清理Message的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMessage(Long id) {
        return required(service.removeMessage(new MessageRemoveParam(id))).saved();
    }

    /**
     * 校验ChatRun完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveChatRun(ChatRunAggregate aggregate) {
        return required(service.saveChatRun(new ChatRunWriteParam(aggregate))).saved();
    }

    /**
     * 清理ChatRun的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeChatRun(Long id) {
        return required(service.removeChatRun(new ChatRunRemoveParam(id))).saved();
    }

    /**
     * 校验MemorySummary完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemorySummary(MemorySummaryAggregate aggregate) {
        return required(service.saveMemorySummary(new MemorySummaryWriteParam(aggregate))).saved();
    }

    /**
     * 清理MemorySummary的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemorySummary(Long id) {
        return required(service.removeMemorySummary(new MemorySummaryRemoveParam(id))).saved();
    }

    /**
     * 校验MemoryFact完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFact(MemoryFactAggregate aggregate) {
        return required(service.saveMemoryFact(new MemoryFactWriteParam(aggregate))).saved();
    }

    /**
     * 清理MemoryFact的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeMemoryFact(Long id) {
        return required(service.removeMemoryFact(new MemoryFactRemoveParam(id))).saved();
    }

    /**
     * 校验MemoryFactSource完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMemoryFactSource(MemoryFactSourceAggregate aggregate) {
        return required(service.saveMemoryFactSource(new MemoryFactSourceWriteParam(aggregate)))
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
        return required(service.removeMemoryFactSource(new MemoryFactSourceRemoveParam(id)))
                .saved();
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeDocument(KnowledgeDocumentAggregate aggregate) {
        return required(service.saveKnowledgeDocument(new KnowledgeDocumentWriteParam(aggregate)))
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
        return required(service.removeKnowledgeDocument(new KnowledgeDocumentRemoveParam(id)))
                .saved();
    }

    /**
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeChunk(KnowledgeChunkAggregate aggregate) {
        return required(service.saveKnowledgeChunk(new KnowledgeChunkWriteParam(aggregate)))
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
        return required(service.removeKnowledgeChunk(new KnowledgeChunkRemoveParam(id))).saved();
    }

    /**
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveIndexJob(IndexJobAggregate aggregate) {
        return required(service.saveIndexJob(new IndexJobWriteParam(aggregate))).saved();
    }

    /**
     * 清理IndexJob的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeIndexJob(Long id) {
        return required(service.removeIndexJob(new IndexJobRemoveParam(id))).saved();
    }

    /**
     * 校验SyncEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveSyncEvent(SyncEventAggregate aggregate) {
        return required(service.saveSyncEvent(new SyncEventWriteParam(aggregate))).saved();
    }

    /**
     * 清理SyncEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeSyncEvent(Long id) {
        return required(service.removeSyncEvent(new SyncEventRemoveParam(id))).saved();
    }

    /**
     * 校验OutboxEvent完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveOutboxEvent(OutboxEventAggregate aggregate) {
        return required(service.saveOutboxEvent(new OutboxEventWriteParam(aggregate))).saved();
    }

    /**
     * 清理OutboxEvent的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeOutboxEvent(Long id) {
        return required(service.removeOutboxEvent(new OutboxEventRemoveParam(id))).saved();
    }

    /**
     * 校验ModelInvocation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveModelInvocation(ModelInvocationAggregate aggregate) {
        return required(service.saveModelInvocation(new ModelInvocationWriteParam(aggregate)))
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
        return required(service.removeModelInvocation(new ModelInvocationRemoveParam(id))).saved();
    }

    /**
     * 校验RefreshReceipt完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveRefreshReceipt(RefreshReceiptAggregate aggregate) {
        return required(service.saveRefreshReceipt(new RefreshReceiptWriteParam(aggregate)))
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
        return required(service.removeRefreshReceipt(new RefreshReceiptRemoveParam(id))).saved();
    }

    private static WriteDO required(Result<WriteDO> result) {
        // 1. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new DomainException(DomainErrorCode.valueOf(result.code()));
        }
        // 2. 返回由领域服务成功结果转换的写标记，调用者据此确认持久化。
        return result.data();
    }
}
