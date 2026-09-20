package com.hellotravel.start;

import static com.hellotravel.start.DomainSnapshots.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hellotravel.application.auth.assembler.AuthDomainParamAssembler;
import com.hellotravel.application.chat.assembler.SyncDomainParamAssembler;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.memory.service.MemoryDomainService;
import com.hellotravel.domain.sync.model.aggregate.SyncEventAggregate;
import com.hellotravel.domain.sync.model.entity.SyncEventEntity;
import com.hellotravel.domain.sync.repository.OutboxEventRepository;
import com.hellotravel.domain.sync.repository.SyncEventRepository;
import com.hellotravel.domain.sync.service.SyncDomainService;
import com.hellotravel.infrastructure.exception.InfrastructureErrorCode;
import com.hellotravel.infrastructure.exception.InfrastructureException;
import com.hellotravel.model.persistence.WriteDO;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** 业务垂直拆分的归属、完整写入、失败分类和跨域事务回归；不访问真实数据库。 */
class VerticalDomainWriteTest {
    private static final List<Class<?>> SERVICES =
            List.of(
                    AuthDomainService.class,
                    ChatDomainService.class,
                    MemoryDomainService.class,
                    KnowledgeDomainService.class,
                    SyncDomainService.class);

    @Test
    void eachDomainOwnsItsRepositoriesAndParameters() {
        int operations = 0;
        for (Class<?> type : SERVICES) {
            String scope = type.getPackageName().replace(".service", "");
            assertEquals(1, type.getConstructors().length);
            for (Class<?> dependency : type.getConstructors()[0].getParameterTypes()) {
                assertEquals(scope + ".repository", dependency.getPackageName());
            }
            for (Method method : entries(type)) {
                assertEquals(
                        scope + ".model.param", method.getParameterTypes()[0].getPackageName());
                operations++;
            }
        }
        assertEquals(20, operations);
    }

    @Test
    void allSeventeenSavesKeepCreationAndUpdateGuards() throws Exception {
        int saves = 0;
        for (Class<?> type : SERVICES) {
            for (Method entry : entries(type)) {
                if (!entry.getName().startsWith("save")) continue;
                var harness = new Harness(type);
                Class<?> aggregateType =
                        entry.getParameterTypes()[0].getRecordComponents()[0].getType();
                Class<?> entityType = aggregateType.getRecordComponents()[0].getType();
                Object prior = snapshot(entityType, Map.of());
                // 新增无需虚构旧记录；更新必须恢复并核对相同完整聚合。
                Object created =
                        aggregateType.getConstructors()[0].newInstance(
                                snapshot(entityType, Collections.singletonMap("id", null)));
                assertTrue(harness.call(entry, created).success());
                assertEquals(0, harness.reads);
                Object stored = aggregateType.getConstructors()[0].newInstance(prior);
                harness.stored = stored;
                assertTrue(harness.call(entry, stored).success());
                assertTrue(harness.reads > 0);
                // 版本、账号归属和公开身份替换在持久化前拒绝。
                for (var field : entityType.getRecordComponents()) {
                    if (!List.of("version", "userId", "publicId").contains(field.getName()))
                        continue;
                    Object changed =
                            snapshot(
                                    entityType,
                                    Map.of(
                                            field.getName(),
                                            field.getName().equals("publicId")
                                                    ? "different"
                                                    : 99L));
                    Object input = aggregateType.getConstructors()[0].newInstance(changed);
                    int before = harness.writes;
                    assertEquals(
                            field.getName().equals("version") ? "CONFLICT" : "INVALID",
                            harness.call(entry, input).code());
                    assertEquals(before, harness.writes);
                }
                // CAS失败、更新目标缺失和底层不可用分别保留稳定分类。
                harness.saveAccepted = false;
                assertEquals("CONFLICT", harness.call(entry, stored).code());
                harness.stored = null;
                int before = harness.writes;
                assertEquals("NOT_FOUND", harness.call(entry, stored).code());
                assertEquals(before, harness.writes);
                harness.storageFailure = true;
                assertEquals("UNAVAILABLE", harness.call(entry, stored).code());
                saves++;
            }
        }
        assertEquals(17, saves);
    }

    @Test
    void usedRetentionRemovalsKeepOutcomesAndFailures() throws Exception {
        int removals = 0;
        for (Class<?> type : SERVICES) {
            for (Method entry : entries(type)) {
                if (!entry.getName().startsWith("remove")) continue;
                var harness = new Harness(type);
                assertTrue(harness.call(entry, 1L).success());
                harness.saveAccepted = false;
                var absent = harness.call(entry, 1L);
                assertTrue(absent.success());
                assertFalse(((WriteDO) absent.data()).saved());
                harness.storageFailure = true;
                assertEquals("UNAVAILABLE", harness.call(entry, 1L).code());
                assertEquals("INVALID", harness.call(entry, null).code());
                removals++;
            }
        }
        assertEquals(3, removals);
    }

    @Test
    void applicationCoordinatesCommitAndRollbackAcrossLocalDomains() throws Exception {
        var accountRepository = mock(UserAccountRepository.class);
        var device = mock(DeviceRepository.class);
        var session = mock(LoginSessionRepository.class);
        var challenge = mock(EmailChallengeRepository.class);
        var refresh = mock(RefreshReceiptRepository.class);
        var eventRepository = mock(SyncEventRepository.class);
        var outbox = mock(OutboxEventRepository.class);
        var authDomainService =
                new AuthDomainService(accountRepository, device, session, challenge, refresh);
        var authDomainParamAssembler = new AuthDomainParamAssembler();
        var syncDomainService = new SyncDomainService(eventRepository, outbox);
        var syncDomainParamAssembler = new SyncDomainParamAssembler();
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        var transactions =
                new Transactions(
                        authDomainService,
                        authDomainParamAssembler,
                        manager,
                        accountRepository);
        var account =
                new UserAccountAggregate(
                        snapshot(UserAccountEntity.class, Collections.singletonMap("id", null)));
        var event =
                new SyncEventAggregate(
                        snapshot(SyncEventEntity.class, Collections.singletonMap("id", null)));
        when(accountRepository.save(any())).thenReturn(true);
        when(eventRepository.save(any())).thenReturn(true);
        transactions.plain(
                () -> {
                    Transactions.require(
                            ApplicationFailures.required(
                                            authDomainService.saveUserAccount(
                                                    authDomainParamAssembler.userAccount(account)))
                                    .saved());
                    Transactions.require(
                            ApplicationFailures.required(
                                            syncDomainService.saveSyncEvent(
                                                    syncDomainParamAssembler.syncEvent(event)))
                                    .saved());
                    return true;
                });
        verify(manager).commit(any());
        clearInvocations(manager, accountRepository, eventRepository);
        when(eventRepository.save(any())).thenReturn(false);
        var afterFailure = new AtomicBoolean();
        var failure =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                transactions.plain(
                                        () -> {
                                            Transactions.require(
                                                    ApplicationFailures.required(
                                                                    authDomainService.saveUserAccount(
                                                                            authDomainParamAssembler
                                                                                    .userAccount(
                                                                                            account)))
                                                            .saved());
                                            Transactions.require(
                                                    ApplicationFailures.required(
                                                                    syncDomainService.saveSyncEvent(
                                                                            syncDomainParamAssembler
                                                                                    .syncEvent(event)))
                                                            .saved());
                                            afterFailure.set(true);
                                            return true;
                                        }));
        assertEquals("CONFLICT", failure.errorCode().code());
        assertFalse(afterFailure.get());
        verify(accountRepository).save(account);
        verify(eventRepository).save(event);
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    private static List<Method> entries(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(
                        m ->
                                Modifier.isPublic(m.getModifiers())
                                        && m.getReturnType() == Result.class)
                .toList();
    }

    private static final class Harness {
        final Object service;
        Object stored;
        boolean saveAccepted = true;
        boolean storageFailure;
        int reads;
        int writes;

        Harness(Class<?> type) throws Exception {
            var constructor = type.getConstructors()[0];
            Object[] repositories =
                    Arrays.stream(constructor.getParameterTypes())
                            .map(
                                    port ->
                                            mock(
                                                    port,
                                                    invocation -> {
                                                        if (storageFailure)
                                                            throw new InfrastructureException(
                                                                    InfrastructureErrorCode
                                                                            .UNAVAILABLE);
                                                        return switch (invocation
                                                                .getMethod()
                                                                .getName()) {
                                                            case "findById" -> {
                                                                reads++;
                                                                yield stored;
                                                            }
                                                            case "save", "remove" -> {
                                                                writes++;
                                                                yield saveAccepted;
                                                            }
                                                            default ->
                                                                    org.mockito.Answers
                                                                            .RETURNS_DEFAULTS
                                                                            .answer(invocation);
                                                        };
                                                    }))
                            .toArray();
            service = constructor.newInstance(repositories);
        }

        Result<?> call(Method entry, Object source) throws Exception {
            var parameterType = entry.getParameterTypes()[0];
            Object parameter = parameterType.getConstructors()[0].newInstance(source);
            return (Result<?>) entry.invoke(service, parameter);
        }
    }
}
