package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hellotravel.application.auth.assembler.AuthApplicationAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.workflow.AuthFlow;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.persistence.DomainWrites;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.assembler.SecurityCommandAssembler;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
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

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 旧页面遇到共享的新刷新Cookie时，必须拒绝绑定并且不撤销新登录。
 *
 * @author AIGenerator
 */
class PageSessionIsolationTest {

    @Test
    void oldPageCannotInheritOrRevokeNewSid() {
        var loginSessions = mock(LoginSessionRepository.class);
        var next =
                new LoginSessionEntity(
                        1L,
                        "NEW-SID",
                        2L,
                        3L,
                        Ids.hash("access"),
                        Ids.hash("refresh"),
                        Ids.hash("new-csrf"),
                        null,
                        "ACTIVE",
                        null,
                        LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15),
                        LocalDateTime.now(ZoneOffset.UTC).plusDays(7),
                        LocalDateTime.now(ZoneOffset.UTC),
                        null,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        0L);
        when(loginSessions.query(any())).thenReturn(List.of(new LoginSessionAggregate(next)));
        var repositories =
                new TravelRepositories(
                        mock(RefreshReceiptRepository.class),
                        mock(UserAccountRepository.class),
                        mock(DeviceRepository.class),
                        loginSessions,
                        mock(EmailChallengeRepository.class),
                        mock(ConversationRepository.class),
                        mock(MessageRepository.class),
                        mock(ChatRunRepository.class),
                        mock(MemorySummaryRepository.class),
                        mock(MemoryFactRepository.class),
                        mock(MemoryFactSourceRepository.class),
                        mock(KnowledgeDocumentRepository.class),
                        mock(KnowledgeChunkRepository.class),
                        mock(IndexJobRepository.class),
                        mock(SyncEventRepository.class),
                        mock(OutboxEventRepository.class),
                        mock(ModelInvocationRepository.class));
        var transactions = mock(Transactions.class);
        var writes = mock(DomainWrites.class);
        var events = mock(SyncEvents.class);
        var security = mock(SecurityOutAdaptor.class);
        var flow =
                new AuthFlow(
                        writes,
                        repositories,
                        transactions,
                        events,
                        security,
                        new AuthApplicationAssembler(),
                        new SecurityCommandAssembler());
        var command =
                new AuthCommand(
                        "REFRESH",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "OLD-SID",
                        null,
                        "refresh",
                        "old-csrf",
                        "test");
        var rejected = assertThrows(ApplicationException.class, () -> flow.perform(command));
        assertEquals(ApplicationErrorCode.SESSION_REPLACED, rejected.errorCode());
        verifyNoInteractions(transactions, writes, events, security);
    }
}
