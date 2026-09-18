package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 领域状态封装行为回归，不调用外部服务或生产数据库。 */
class DomainModelQualityTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 9, 18, 8, 0);
    private static final List<String> ENTITIES =
            List.of(
                    "auth.UserAccount",
                    "auth.Device",
                    "auth.LoginSession",
                    "auth.EmailChallenge",
                    "auth.RefreshReceipt",
                    "chat.Conversation",
                    "chat.Message",
                    "chat.ChatRun",
                    "chat.ModelInvocation",
                    "knowledge.KnowledgeDocument",
                    "knowledge.KnowledgeChunk",
                    "knowledge.IndexJob",
                    "memory.MemorySummary",
                    "memory.MemoryFact",
                    "memory.MemoryFactSource",
                    "sync.SyncEvent",
                    "sync.OutboxEvent");

    @Test
    void creationCannotBypassOwnedTitleOrVerifiedAccountRules() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 0, 0);
        var conversation = ConversationAggregate.started(1L, "  景德镇  ", now, now, now);
        assertEquals("景德镇", conversation.entity().title());
        assertEquals("瓷都", conversation.rename("  瓷都  ").entity().title());
        assertThrows(
                IllegalArgumentException.class,
                () -> ConversationAggregate.started(1L, " ", now, now, now));
        assertThrows(
                IllegalArgumentException.class,
                () -> ConversationAggregate.started(null, "旅行", now, now, now));
        assertThrows(IllegalArgumentException.class, () -> conversation.rename("x".repeat(121)));
        assertThrows(
                IllegalArgumentException.class,
                () -> UserAccountAggregate.registered("a@example.test", "hash", null, now, now));
        var account = UserAccountAggregate.registered("a@example.test", "hash", now, now, now);
        assertEquals("ACTIVE", account.entity().status());
        assertEquals(0L, account.entity().authEpoch());
    }

    @Test
    void allSeventeenEntitiesRejectVersionAndOwnershipReplacement() throws Exception {
        // 1. 同快照更新应可行，全部17类更新行为实际执行。
        for (String name : ENTITIES) {
            String[] parts = name.split("\\.");
            Class<?> type =
                    Class.forName(
                            "com.hellotravel.domain."
                                    + parts[0]
                                    + ".model.entity."
                                    + parts[1]
                                    + "Entity");
            Object prior = snapshot(type, Map.of());
            var guard = type.getMethod("assertUpdateAgainst", type);
            guard.invoke(prior, prior);
            // 2. 并发版本、账号归属和公开标识不能被调用方替换。
            for (RecordComponent field : type.getRecordComponents()) {
                if (!List.of("version", "userId", "publicId").contains(field.getName())) continue;
                Object changed =
                        snapshot(
                                type,
                                Map.of(
                                        field.getName(),
                                        field.getName().equals("publicId")
                                                ? "other-public-id"
                                                : 99L));
                InvocationTargetException failure =
                        assertThrows(
                                InvocationTargetException.class,
                                () -> guard.invoke(changed, prior),
                                name + "." + field.getName());
                assertInstanceOf(DomainException.class, failure.getCause());
                assertEquals(
                        field.getName().equals("version") ? "CONFLICT" : "INVALID",
                        ((DomainException) failure.getCause()).errorCode().code());
            }
        }
    }

    @Test
    void submittedTurnNeedsOwnedPersistedMessagePair() throws Exception {
        // 1. 角色和稳定顺序由领域对象控制，任务不得关联未保存消息。
        var conversation = snapshot(ConversationEntity.class, Map.of("lastMessageSeq", 5L));
        var input = MessageAggregate.userInput(conversation, "计划景德镇两日游", TIME).entity();
        var output = MessageAggregate.assistantPlaceholder(conversation, TIME).entity();
        assertEquals("USER", input.role());
        assertEquals(6L, input.messageSeq());
        assertEquals(7L, output.messageSeq());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ChatRunAggregate.accepted(
                                conversation, 1L, "request", new byte[32], input, output, TIME));
        // 2. 正常消息对绑定当前会话和记忆代次，跨账号消息被拒绝。
        var storedInput = copy(input, Map.of("id", 2L));
        var storedOutput = copy(output, Map.of("id", 3L));
        var run =
                ChatRunAggregate.accepted(
                                conversation,
                                1L,
                                "request",
                                new byte[32],
                                storedInput,
                                storedOutput,
                                TIME)
                        .entity();
        assertEquals("ACCEPTED", run.status());
        assertEquals(conversation.memoryEpoch(), run.memoryEpochAtStart());
        var foreign = copy(storedInput, Map.of("userId", 99L));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ChatRunAggregate.accepted(
                                conversation,
                                1L,
                                "request",
                                new byte[32],
                                foreign,
                                storedOutput,
                                TIME));
    }

    @Test
    void issuedSessionOwnsCredentialCopiesAndExpiryPolicy() throws Exception {
        // 1. 签发由实体封装摘要，调用方不能改变已签发凭据。
        var user = snapshot(UserAccountEntity.class, Map.of());
        var device = snapshot(DeviceEntity.class, Map.of());
        byte[] access = new byte[32], refresh = new byte[32], csrf = new byte[32];
        var session =
                LoginSessionAggregate.issue(user, device, "sid", access, refresh, csrf, TIME)
                        .entity();
        access[0] = 5;
        byte[] returned = session.accessTokenHash();
        returned[0] = 6;
        assertArrayEquals(new byte[32], session.accessTokenHash());
        // 2. 有效期固定且设备必须属于当前账号。
        assertEquals(TIME.plusMinutes(15), session.accessExpiresAt());
        assertEquals(TIME.plusDays(7), session.refreshExpiresAt());
        var foreign = copy(device, Map.of("userId", 99L));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        LoginSessionAggregate.issue(
                                user, foreign, "sid", access, refresh, csrf, TIME));
    }

    @Test
    void factProposalRejectsHallucinationAndKeepsScopeOnRevision() throws Exception {
        // 1. 原文外推测与未允许类别不得进入长期记忆。
        assertTrue(MemoryFactAggregate.propose("budget", "PREFERENCE", "预算一千元", "计划两日游").isEmpty());
        assertTrue(MemoryFactAggregate.propose("duration", "SECRET", "两日游", "计划两日游").isEmpty());
        var proposal =
                MemoryFactAggregate.propose("duration", "TRAVEL_CONSTRAINT", "两日游", "请记住计划两日游")
                        .orElseThrow();
        var run = snapshot(ChatRunEntity.class, Map.of());
        var fact = MemoryFactAggregate.explicit(run, proposal, TIME).entity();
        // 2. 修订保留用户、会话、代次和原数据库版本。
        var revised =
                new MemoryFactAggregate(fact).reviseExplicit(proposal, TIME.plusDays(1)).entity();
        assertEquals(fact.userId(), revised.userId());
        assertEquals(fact.conversationId(), revised.conversationId());
        assertEquals(fact.memoryEpoch(), revised.memoryEpoch());
        assertEquals(fact.version(), revised.version());
        assertEquals(TIME.plusDays(91), revised.expiresAt());
    }

    @Test
    void indexLeaseAndChunkTransitionsRemainInDomain() throws Exception {
        // 1. 领取提高栅栏，已领取任务不能再次领取。
        var pending =
                snapshot(IndexJobEntity.class, Map.of("status", "PENDING", "attemptCount", 0));
        var claimed = new IndexJobAggregate(pending).claim("worker", TIME).entity();
        assertEquals("RUNNING", claimed.status());
        assertEquals(pending.leaseFence() + 1, claimed.leaseFence());
        assertThrows(
                IllegalStateException.class,
                () -> new IndexJobAggregate(claimed).claim("other", TIME));
        // 2. 分块键与向量键一次创建；完成索引不改变正文或版本。
        var chunk =
                KnowledgeChunkAggregate.pending(claimed, 0, "退改条款原文", new byte[32], 20, TIME)
                        .entity();
        assertNotNull(chunk.publicId());
        assertEquals(chunk.publicId(), chunk.vectorKey());
        var ready = new KnowledgeChunkAggregate(chunk).ready().entity();
        assertEquals("READY", ready.status());
        assertEquals(chunk.content(), ready.content());
        assertEquals(chunk.version(), ready.version());
    }

    @SuppressWarnings("unchecked")
    private static <T> T snapshot(Class<T> type, Map<String, Object> replacements)
            throws Exception {
        RecordComponent[] fields = type.getRecordComponents();
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            var field = fields[i];
            Object value =
                    field.getType() == Long.class
                            ? 1L
                            : field.getType() == Integer.class
                                    ? 1
                                    : field.getType() == String.class
                                            ? "ACTIVE"
                                            : field.getType() == byte[].class
                                                    ? new byte[32]
                                                    : field.getType() == LocalDateTime.class
                                                            ? TIME
                                                            : null;
            if (field.getName().equals("version")) value = 0L;
            if (List.of("deletedAt", "revokedAt").contains(field.getName())) value = null;
            values[i] = replacements.getOrDefault(field.getName(), value);
        }
        return (T)
                type.getDeclaredConstructor(
                                Arrays.stream(fields)
                                        .map(RecordComponent::getType)
                                        .toArray(Class<?>[]::new))
                        .newInstance(values);
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(T source, Map<String, Object> replacements) throws Exception {
        var type = source.getClass();
        var fields = type.getRecordComponents();
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++)
            values[i] =
                    replacements.getOrDefault(
                            fields[i].getName(), fields[i].getAccessor().invoke(source));
        return (T)
                type.getDeclaredConstructor(
                                Arrays.stream(fields)
                                        .map(RecordComponent::getType)
                                        .toArray(Class<?>[]::new))
                        .newInstance(values);
    }
}
