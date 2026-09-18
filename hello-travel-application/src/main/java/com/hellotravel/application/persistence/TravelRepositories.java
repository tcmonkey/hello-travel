package com.hellotravel.application.persistence;

import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.memory.repository.MemoryFactRepository;
import com.hellotravel.domain.memory.repository.MemoryFactSourceRepository;
import com.hellotravel.domain.memory.repository.MemorySummaryRepository;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;

import org.springframework.stereotype.Component;

/**
 * 应用用例依赖的强类型领域仓储装配，不包含PO或Mapper。
 *
 * @author AIGenerator
 */
@Component
public final class TravelRepositories {

    public final RefreshReceiptRepository refreshReceipt;

    public final UserAccountRepository userAccount;

    public final DeviceRepository device;

    public final LoginSessionRepository loginSession;

    public final EmailChallengeRepository emailChallenge;

    public final ConversationRepository conversation;

    public final MessageRepository message;

    public final ChatRunRepository chatRun;

    public final MemorySummaryRepository memorySummary;

    public final MemoryFactRepository memoryFact;

    public final MemoryFactSourceRepository memoryFactSource;

    public final KnowledgeDocumentRepository knowledgeDocument;

    public final KnowledgeChunkRepository knowledgeChunk;

    public final IndexJobRepository indexJob;

    public final SyncEventRepository syncEvent;

    public final OutboxEventRepository outboxEvent;

    public final ModelInvocationRepository modelInvocation;

    public TravelRepositories(
            RefreshReceiptRepository refreshReceipt,
            UserAccountRepository userAccount,
            DeviceRepository device,
            LoginSessionRepository loginSession,
            EmailChallengeRepository emailChallenge,
            ConversationRepository conversation,
            MessageRepository message,
            ChatRunRepository chatRun,
            MemorySummaryRepository memorySummary,
            MemoryFactRepository memoryFact,
            MemoryFactSourceRepository memoryFactSource,
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob,
            SyncEventRepository syncEvent,
            OutboxEventRepository outboxEvent,
            ModelInvocationRepository modelInvocation) {
        this.refreshReceipt = refreshReceipt;
        this.userAccount = userAccount;
        this.device = device;
        this.loginSession = loginSession;
        this.emailChallenge = emailChallenge;
        this.conversation = conversation;
        this.message = message;
        this.chatRun = chatRun;
        this.memorySummary = memorySummary;
        this.memoryFact = memoryFact;
        this.memoryFactSource = memoryFactSource;
        this.knowledgeDocument = knowledgeDocument;
        this.knowledgeChunk = knowledgeChunk;
        this.indexJob = indexJob;
        this.syncEvent = syncEvent;
        this.outboxEvent = outboxEvent;
        this.modelInvocation = modelInvocation;
    }
}
