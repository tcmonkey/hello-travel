package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 编码阶段的离线不变量检查，不调用外部服务。
 *
 * @author AIGenerator
 */
class DomainInvariantsTest {

    @Test
    void exactBudgetBoundaryAndOverflow() {
        assertTrue(new ContextBudgetValue(32768, 24576, 4096, 4096).fits());
        assertFalse(new ContextBudgetValue(32768, 24577, 4096, 4096).fits());
        assertFalse(new ContextBudgetValue(32768, Integer.MAX_VALUE, 4096, 4096).fits());
    }

    @Test
    void chineseBudgetIncludesUtf8AndPerMessageSafety() {
        assertEquals(70, ContextBudgetValue.estimate("旅行"));
        assertThrows(
                IllegalArgumentException.class, () -> new ContextBudgetValue(4096, 0, 1024, 0));
    }

    @Test
    void deletionAdvancesBothEpochs() {
        var original =
                new ConversationEntity(
                        1L,
                        com.hellotravel.common.identity.Ids.next(),
                        2L,
                        "测试",
                        0L,
                        7L,
                        9L,
                        LocalDateTime.now(ZoneOffset.UTC),
                        null,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        0L);
        var deleted = original.eraseHistory(true);
        assertEquals(8L, deleted.historyEpoch());
        assertEquals(10L, deleted.memoryEpoch());
        assertNotNull(deleted.deletedAt());
    }

    @Test
    void retryKeepsMessagePositionsAndAdvancesFence() {
        var original =
                new ChatRunEntity(
                        1L,
                        com.hellotravel.common.identity.Ids.next(),
                        2L,
                        3L,
                        4L,
                        "001",
                        new byte[32],
                        5L,
                        6L,
                        "FAILED",
                        1,
                        null,
                        "travel-v1",
                        7L,
                        null,
                        null,
                        null,
                        4L,
                        null,
                        null,
                        null,
                        null,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        0L);
        var retried = original.retry(8L);
        assertEquals(original.userMessageId(), retried.userMessageId());
        assertEquals(original.assistantMessageId(), retried.assistantMessageId());
        assertEquals(2, retried.attemptCount());
        assertEquals(5L, retried.leaseFence());
        assertEquals(8L, retried.memoryEpochAtStart());
        assertEquals("ACCEPTED", retried.status());
        assertThrows(IllegalStateException.class, () -> retried.retry(8L));
    }

    @Test
    void revocationErasesAllActiveHashes() {
        var original =
                new LoginSessionEntity(
                        1L,
                        com.hellotravel.common.identity.Ids.next(),
                        2L,
                        3L,
                        new byte[32],
                        new byte[32],
                        new byte[32],
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
        var revoked = original.revoke("REPLACED");
        assertNull(revoked.accessTokenHash());
        assertNull(revoked.refreshTokenHash());
        assertNull(revoked.csrfTokenHash());
        assertEquals("REVOKED", revoked.status());
    }
}
