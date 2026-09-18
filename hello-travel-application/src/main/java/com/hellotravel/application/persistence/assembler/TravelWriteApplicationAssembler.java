package com.hellotravel.application.persistence.assembler;

import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
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
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;

import org.springframework.stereotype.Component;

/**
 * 应用到领域持久化参数的类型安全映射。
 *
 * @author AIGenerator
 */
@Component
public final class TravelWriteApplicationAssembler {

    /**
     * 投影EmailChallenge写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public EmailChallengeWriteParam write(EmailChallengeAggregate aggregate) {
        return new EmailChallengeWriteParam(aggregate);
    }

    /**
     * 投影EmailChallenge删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public EmailChallengeRemoveParam removeEmailChallenge(Long id) {
        return new EmailChallengeRemoveParam(id);
    }

    /**
     * 投影KnowledgeChunk写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeChunkWriteParam write(KnowledgeChunkAggregate aggregate) {
        return new KnowledgeChunkWriteParam(aggregate);
    }

    /**
     * 投影KnowledgeChunk删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeChunkRemoveParam removeKnowledgeChunk(Long id) {
        return new KnowledgeChunkRemoveParam(id);
    }

    /**
     * 投影MemoryFact写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactWriteParam write(MemoryFactAggregate aggregate) {
        return new MemoryFactWriteParam(aggregate);
    }

    /**
     * 投影MemoryFact删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactRemoveParam removeMemoryFact(Long id) {
        return new MemoryFactRemoveParam(id);
    }

    /**
     * 投影LoginSession写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public LoginSessionWriteParam write(LoginSessionAggregate aggregate) {
        return new LoginSessionWriteParam(aggregate);
    }

    /**
     * 投影LoginSession删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public LoginSessionRemoveParam removeLoginSession(Long id) {
        return new LoginSessionRemoveParam(id);
    }

    /**
     * 投影SyncEvent写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventWriteParam write(SyncEventAggregate aggregate) {
        return new SyncEventWriteParam(aggregate);
    }

    /**
     * 投影SyncEvent删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SyncEventRemoveParam removeSyncEvent(Long id) {
        return new SyncEventRemoveParam(id);
    }

    /**
     * 投影ChatRun写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatRunWriteParam write(ChatRunAggregate aggregate) {
        return new ChatRunWriteParam(aggregate);
    }

    /**
     * 投影ChatRun删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatRunRemoveParam removeChatRun(Long id) {
        return new ChatRunRemoveParam(id);
    }

    /**
     * 投影ModelInvocation写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ModelInvocationWriteParam write(ModelInvocationAggregate aggregate) {
        return new ModelInvocationWriteParam(aggregate);
    }

    /**
     * 投影ModelInvocation删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ModelInvocationRemoveParam removeModelInvocation(Long id) {
        return new ModelInvocationRemoveParam(id);
    }

    /**
     * 投影UserAccount写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public UserAccountWriteParam write(UserAccountAggregate aggregate) {
        return new UserAccountWriteParam(aggregate);
    }

    /**
     * 投影UserAccount删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public UserAccountRemoveParam removeUserAccount(Long id) {
        return new UserAccountRemoveParam(id);
    }

    /**
     * 投影Conversation写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ConversationWriteParam write(ConversationAggregate aggregate) {
        return new ConversationWriteParam(aggregate);
    }

    /**
     * 投影Conversation删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ConversationRemoveParam removeConversation(Long id) {
        return new ConversationRemoveParam(id);
    }

    /**
     * 投影IndexJob写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public IndexJobWriteParam write(IndexJobAggregate aggregate) {
        return new IndexJobWriteParam(aggregate);
    }

    /**
     * 投影IndexJob删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public IndexJobRemoveParam removeIndexJob(Long id) {
        return new IndexJobRemoveParam(id);
    }

    /**
     * 投影MemoryFactSource写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactSourceWriteParam write(MemoryFactSourceAggregate aggregate) {
        return new MemoryFactSourceWriteParam(aggregate);
    }

    /**
     * 投影MemoryFactSource删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemoryFactSourceRemoveParam removeMemoryFactSource(Long id) {
        return new MemoryFactSourceRemoveParam(id);
    }

    /**
     * 投影KnowledgeDocument写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeDocumentWriteParam write(KnowledgeDocumentAggregate aggregate) {
        return new KnowledgeDocumentWriteParam(aggregate);
    }

    /**
     * 投影KnowledgeDocument删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeDocumentRemoveParam removeKnowledgeDocument(Long id) {
        return new KnowledgeDocumentRemoveParam(id);
    }

    /**
     * 投影Device写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DeviceWriteParam write(DeviceAggregate aggregate) {
        return new DeviceWriteParam(aggregate);
    }

    /**
     * 投影Device删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DeviceRemoveParam removeDevice(Long id) {
        return new DeviceRemoveParam(id);
    }

    /**
     * 投影Message写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MessageWriteParam write(MessageAggregate aggregate) {
        return new MessageWriteParam(aggregate);
    }

    /**
     * 投影Message删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MessageRemoveParam removeMessage(Long id) {
        return new MessageRemoveParam(id);
    }

    /**
     * 投影MemorySummary写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemorySummaryWriteParam write(MemorySummaryAggregate aggregate) {
        return new MemorySummaryWriteParam(aggregate);
    }

    /**
     * 投影MemorySummary删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MemorySummaryRemoveParam removeMemorySummary(Long id) {
        return new MemorySummaryRemoveParam(id);
    }

    /**
     * 投影OutboxEvent写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public OutboxEventWriteParam write(OutboxEventAggregate aggregate) {
        return new OutboxEventWriteParam(aggregate);
    }

    /**
     * 投影OutboxEvent删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public OutboxEventRemoveParam removeOutboxEvent(Long id) {
        return new OutboxEventRemoveParam(id);
    }

    /**
     * 投影RefreshReceipt写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public RefreshReceiptWriteParam write(RefreshReceiptAggregate aggregate) {
        return new RefreshReceiptWriteParam(aggregate);
    }

    /**
     * 投影RefreshReceipt删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public RefreshReceiptRemoveParam removeRefreshReceipt(Long id) {
        return new RefreshReceiptRemoveParam(id);
    }
}
