package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hellotravel.application.auth.assembler.AuthApplicationAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.support.AuthRepositories;
import com.hellotravel.application.auth.support.AuthWrites;
import com.hellotravel.application.auth.usecase.AuthActionOperations;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.assembler.SecurityCommandAssembler;
import com.hellotravel.application.sync.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;

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
                new AuthRepositories(
                        mock(UserAccountRepository.class),
                        mock(DeviceRepository.class),
                        loginSessions,
                        mock(EmailChallengeRepository.class),
                        mock(RefreshReceiptRepository.class));
        var transactions = mock(Transactions.class);
        var writes = mock(AuthWrites.class);
        var events = mock(SyncEventPublisher.class);
        var security = mock(SecurityOutAdaptor.class);
        var operations =
                new AuthActionOperations(
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
        var rejected = assertThrows(ApplicationException.class, () -> operations.refresh(command));
        assertEquals(ApplicationErrorCode.SESSION_REPLACED, rejected.errorCode());
        verifyNoInteractions(transactions, writes, events, security);
    }
}
