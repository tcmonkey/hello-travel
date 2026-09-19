package com.hellotravel.start.config;

import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.domain.memory.service.MemoryDomainService;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;
import com.hellotravel.domain.sync.service.SyncDomainService;
import com.hellotravel.domain.travel.service.TravelPlanDomainService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按业务子域显式装配领域服务，领域模块不依赖Spring。
 *
 * @author AIGenerator
 */
@Configuration
public class DomainConfiguration {

    /**
     * 装配认证业务领域写入服务，只传入本域仓储。
     *
     * @param userAccount UserAccount本域仓储
     * @param device Device本域仓储
     * @param loginSession LoginSession本域仓储
     * @param emailChallenge EmailChallenge本域仓储
     * @param refreshReceipt RefreshReceipt本域仓储
     * @return 认证领域写入服务
     * @author AIGenerator
     */
    @Bean
    public AuthDomainService authDomainService(
            UserAccountRepository userAccount,
            DeviceRepository device,
            LoginSessionRepository loginSession,
            EmailChallengeRepository emailChallenge,
            RefreshReceiptRepository refreshReceipt) {
        return new AuthDomainService(
                userAccount, device, loginSession, emailChallenge, refreshReceipt);
    }

    /**
     * 装配对话业务领域写入服务，只传入本域仓储。
     *
     * @param conversation Conversation本域仓储
     * @param message Message本域仓储
     * @param chatRun ChatRun本域仓储
     * @param modelInvocation ModelInvocation本域仓储
     * @return 对话领域写入服务
     * @author AIGenerator
     */
    @Bean
    public ChatDomainService chatDomainService(
            ConversationRepository conversation,
            MessageRepository message,
            ChatRunRepository chatRun,
            ModelInvocationRepository modelInvocation) {
        return new ChatDomainService(conversation, message, chatRun, modelInvocation);
    }

    /**
     * 装配记忆业务领域写入服务，只传入本域仓储。
     *
     * @param memorySummary MemorySummary本域仓储
     * @param memoryFact MemoryFact本域仓储
     * @param memoryFactSource MemoryFactSource本域仓储
     * @return 记忆领域写入服务
     * @author AIGenerator
     */
    @Bean
    public MemoryDomainService memoryDomainService(
            MemorySummaryRepository memorySummary,
            MemoryFactRepository memoryFact,
            MemoryFactSourceRepository memoryFactSource) {
        return new MemoryDomainService(memorySummary, memoryFact, memoryFactSource);
    }

    /**
     * 装配知识库业务领域写入服务，只传入本域仓储。
     *
     * @param knowledgeDocument KnowledgeDocument本域仓储
     * @param knowledgeChunk KnowledgeChunk本域仓储
     * @param indexJob IndexJob本域仓储
     * @return 知识库领域写入服务
     * @author AIGenerator
     */
    @Bean
    public KnowledgeDomainService knowledgeDomainService(
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob) {
        return new KnowledgeDomainService(knowledgeDocument, knowledgeChunk, indexJob);
    }

    /**
     * 装配同步业务领域写入服务，只传入本域仓储。
     *
     * @param syncEvent SyncEvent本域仓储
     * @param outboxEvent OutboxEvent本域仓储
     * @return 同步领域写入服务
     * @author AIGenerator
     */
    @Bean
    public SyncDomainService syncDomainService(
            SyncEventRepository syncEvent, OutboxEventRepository outboxEvent) {
        return new SyncDomainService(syncEvent, outboxEvent);
    }

    /**
     * 装配旅行规划确定性规则服务。
     *
     * @return 旅行规划领域服务
     * @author AIGenerator
     */
    @Bean
    public TravelPlanDomainService travelPlanDomainService() {
        return new TravelPlanDomainService();
    }
}
