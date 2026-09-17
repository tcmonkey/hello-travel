package com.hellotravel.application.chat.workflow;

import com.hellotravel.application.chat.assembler.ChatApplicationAssembler;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.chat.result.ConversationResult;
import com.hellotravel.application.chat.result.MessageResult;
import com.hellotravel.application.chat.result.RunResult;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对话生命周期、完整keyset恢复、幂等提交和任务终态编排。
 *
 * @author AIGenerator
 */
@Component
public final class ChatFlow {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    private final ChatApplicationAssembler assembler;

    public ChatFlow(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events,
            ChatApplicationAssembler assembler) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
        this.assembler = assembler;
    }

    /**
     * 分发可信业务动作。
     *
     * @author AIGenerator
     * @param command 受控command参数
     * @return 当前操作的业务结果
     */
    public ChatResult perform(ChatCommand command) {
        if (command.limit() < 1 || command.limit() > 200) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        return switch (command.action()) {
            case "BOOTSTRAP" -> bootstrap(command);
            case "LIST" -> list(command);
            case "CREATE" -> create(command);
            case "RENAME" -> rename(command);
            case "DELETE" -> erase(command, true);
            case "DELETE_MESSAGES" -> erase(command, false);
            case "HISTORY" -> history(command);
            case "SUBMIT" -> submit(command);
            case "RUN" -> readRun(command);
            case "CANCEL" -> cancel(command);
            case "RETRY" -> retry(command);
            case "CONTEXT" -> context(command);
            default -> throw new DomainException(DomainErrorCode.INVALID);
        };
    }

    /**
     * 读取指定账号拥有的未删除对象。
     *
     * @author AIGenerator
     * @param userId 认证账号主键
     * @param publicId 受控publicId参数
     * @return 归属和状态校验后的业务快照
     */
    public ConversationEntity owned(Long userId, String publicId) {
        var found =
                repositories.conversation.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", userId)
                                .where("public_id", "EQ", publicId)
                                .where("deleted_at", "NULL", null));
        if (found.isEmpty()) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        return found.get(0).entity();
    }

    private ChatRunEntity run(ChatCommand command) {
        var found =
                repositories.chatRun.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", command.userId())
                                .where("public_id", "EQ", command.runId()));
        if (found.isEmpty()) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        ConversationEntity conversation =
                repositories.conversation.findById(found.get(0).entity().conversationId()).entity();
        if (conversation.deletedAt() != null) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        return found.get(0).entity();
    }

    private void version(ConversationEntity c, Long expected) {
        if (expected == null || !c.version().equals(expected)) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }

    private List<ChatRunAggregate> active(ConversationEntity c) {
        return repositories.chatRun.query(
                QueryValue.all("id", 1)
                        .where("conversation_id", "EQ", c.id())
                        .where("status", "IN", List.of("ACCEPTED", "RUNNING")));
    }

    private ChatResult result(
            List<ConversationResult> tabs,
            List<MessageResult> messages,
            RunResult run,
            long cursor,
            boolean more,
            long max,
            long epoch,
            long sync,
            String context) {
        return new ChatResult(tabs, messages, run, cursor, more, max, epoch, sync, context);
    }

    private ChatResult bootstrap(ChatCommand command) {
        return transactions.snapshot(
                () -> {
                    var newest =
                            repositories.conversation.query(
                                    QueryValue.all("id", 1)
                                            .desc()
                                            .where("user_id", "EQ", command.userId()));
                    long max = newest.isEmpty() ? 0 : newest.get(0).entity().id();
                    long sync =
                            repositories.userAccount.findById(command.userId()).entity().syncSeq();
                    return result(List.of(), List.of(), null, 0, false, max, 0, sync, null);
                });
    }

    private ChatResult list(ChatCommand command) {
        QueryValue query =
                QueryValue.all("id", command.limit())
                        .where("user_id", "EQ", command.userId())
                        .where("deleted_at", "NULL", null)
                        .where("id", "GT", command.after());
        if (command.maxSeq() != null) {
            query = query.where("id", "LE", command.maxSeq());
        }
        var rows = repositories.conversation.query(query);
        long cursor = rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().id();
        return result(
                rows.stream().map(x -> assembler.conversation(x.entity())).toList(),
                List.of(),
                null,
                cursor,
                rows.size() == command.limit(),
                command.maxSeq() == null ? 0 : command.maxSeq(),
                0,
                0,
                null);
    }

    private ChatResult create(ChatCommand command) {
        String name = command.title() == null ? "新对话" : command.title();
        return transactions.mutate(
                command.userId(),
                account -> {
                    ConversationEntity created =
                            new ConversationEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    account.id(),
                                    name,
                                    0L,
                                    0L,
                                    0L,
                                    now(),
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    created = created.rename(name);
                    Transactions.require(
                            writes.saveConversation(new ConversationAggregate(created)));
                    events.append(
                            account, "conversation.created", created.publicId(), 0, null, "{}");
                    return result(
                            List.of(assembler.conversation(created)),
                            List.of(),
                            null,
                            0,
                            false,
                            0,
                            0,
                            account.syncSeq(),
                            null);
                });
    }

    private ChatResult rename(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    ConversationEntity stored = owned(account.id(), command.conversationId());
                    version(stored, command.expectedVersion());
                    Transactions.require(
                            writes.saveConversation(
                                    new ConversationAggregate(stored.rename(command.title()))));
                    events.append(
                            account,
                            "conversation.renamed",
                            stored.publicId(),
                            stored.version() + 1,
                            null,
                            "{}");
                    ConversationEntity current = owned(account.id(), stored.publicId());
                    return result(
                            List.of(assembler.conversation(current)),
                            List.of(),
                            null,
                            0,
                            false,
                            0,
                            0,
                            account.syncSeq(),
                            null);
                });
    }

    private ChatResult history(ChatCommand command) {
        ConversationEntity c = owned(command.userId(), command.conversationId());
        if (command.historyEpoch() != null && !c.historyEpoch().equals(command.historyEpoch())) {
            throw new DomainException(DomainErrorCode.SYNC_RESET_REQUIRED);
        }
        long max = command.maxSeq() == null ? c.lastMessageSeq() : command.maxSeq();
        var rows =
                repositories.message.query(
                        QueryValue.all("message_seq", command.limit())
                                .where("user_id", "EQ", command.userId())
                                .where("conversation_id", "EQ", c.id())
                                .where("deleted_at", "NULL", null)
                                .where("message_seq", "GT", command.after())
                                .where("message_seq", "LE", max));
        // 再读删除代次防止分页期间删除；普通流式更新不会触发重启。
        if (!repositories
                .conversation
                .findById(c.id())
                .entity()
                .historyEpoch()
                .equals(c.historyEpoch())) {
            throw new DomainException(DomainErrorCode.SYNC_RESET_REQUIRED);
        }
        long cursor =
                rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().messageSeq();
        return result(
                List.of(assembler.conversation(c)),
                rows.stream().map(x -> assembler.message(x.entity())).toList(),
                null,
                cursor,
                rows.size() == command.limit() && cursor < max,
                max,
                c.historyEpoch(),
                0,
                null);
    }

    private synchronized ChatResult submit(ChatCommand command) {
        if (command.text() == null || command.text().isBlank() || command.text().length() > 8000) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        try {
            UUID.fromString(command.requestKey());
        } catch (RuntimeException e) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        byte[] digest = Ids.hash(command.text());
        return transactions.mutate(
                command.userId(),
                account -> {
                    ConversationEntity c = owned(account.id(), command.conversationId());
                    var replay =
                            repositories.chatRun.query(
                                    QueryValue.all("id", 1)
                                            .where("user_id", "EQ", account.id())
                                            .where("conversation_id", "EQ", c.id())
                                            .where("request_key", "EQ", command.requestKey()));
                    if (!replay.isEmpty()) {
                        if (!java.security.MessageDigest.isEqual(
                                digest, replay.get(0).entity().requestDigest())) {
                            throw new DomainException(DomainErrorCode.IDEMPOTENCY_CONFLICT);
                        }
                        // 即使幂等重放也提交一个无正文事件，保持账号序列没有补齐缺口。
                        events.append(
                                account,
                                "run.replayed",
                                replay.get(0).entity().publicId(),
                                replay.get(0).entity().version(),
                                null,
                                "{}");
                        return readRun(commandForRun(command, replay.get(0).entity().publicId()));
                    }
                    if (repositories
                                    .chatRun
                                    .query(
                                            QueryValue.all("id", 8)
                                                    .where(
                                                            "status",
                                                            "IN",
                                                            List.of("ACCEPTED", "RUNNING")))
                                    .size()
                            >= 8) {
                        throw new DomainException(DomainErrorCode.RATE_LIMITED);
                    }
                    if (!active(c).isEmpty()
                            || !repositories
                                    .chatRun
                                    .query(
                                            QueryValue.all("id", 1)
                                                    .where("user_id", "EQ", command.userId())
                                                    .where(
                                                            "status",
                                                            "IN",
                                                            List.of("ACCEPTED", "RUNNING")))
                                    .isEmpty()) {
                        throw new DomainException(DomainErrorCode.BUSY);
                    }
                    MessageEntity input =
                            new MessageEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    account.id(),
                                    c.id(),
                                    c.lastMessageSeq() + 1,
                                    "USER",
                                    "COMPLETED",
                                    command.text(),
                                    null,
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    MessageEntity output =
                            new MessageEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    account.id(),
                                    c.id(),
                                    c.lastMessageSeq() + 2,
                                    "ASSISTANT",
                                    null,
                                    "",
                                    null,
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(
                            writes.saveConversation(new ConversationAggregate(c.appendPair())));
                    Transactions.require(writes.saveMessage(new MessageAggregate(input)));
                    Transactions.require(writes.saveMessage(new MessageAggregate(output)));
                    MessageEntity storedInput =
                            repositories
                                    .message
                                    .query(
                                            QueryValue.all("id", 1)
                                                    .where("public_id", "EQ", input.publicId()))
                                    .get(0)
                                    .entity();
                    MessageEntity storedOutput =
                            repositories
                                    .message
                                    .query(
                                            QueryValue.all("id", 1)
                                                    .where("public_id", "EQ", output.publicId()))
                                    .get(0)
                                    .entity();
                    ChatRunEntity run =
                            new ChatRunEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    account.id(),
                                    c.id(),
                                    command.sessionId(),
                                    command.requestKey(),
                                    digest,
                                    storedInput.id(),
                                    storedOutput.id(),
                                    "ACCEPTED",
                                    0,
                                    null,
                                    "travel-v1",
                                    c.memoryEpoch(),
                                    null,
                                    null,
                                    null,
                                    0L,
                                    null,
                                    null,
                                    null,
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(run)));
                    events.append(
                            account,
                            "messages.accepted",
                            c.publicId(),
                            c.version() + 1,
                            null,
                            Json.encode(Map.of("runId", run.publicId())));
                    events.outbox(
                            account.id(),
                            "GENERATE",
                            run.publicId(),
                            Json.encode(Map.of("runId", run.publicId())));
                    return result(
                            List.of(),
                            List.of(assembler.message(input), assembler.message(output)),
                            assembler.run(run, c.publicId()),
                            0,
                            false,
                            0,
                            0,
                            account.syncSeq(),
                            null);
                });
    }

    private ChatCommand commandForRun(ChatCommand c, String runId) {
        return new ChatCommand(
                "RUN",
                c.userId(),
                c.sessionId(),
                null,
                runId,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                100);
    }

    private ChatResult readRun(ChatCommand command) {
        ChatRunEntity r = run(command);
        ConversationEntity c = repositories.conversation.findById(r.conversationId()).entity();
        MessageEntity output = repositories.message.findById(r.assistantMessageId()).entity();
        return result(
                List.of(),
                output.deletedAt() == null ? List.of(assembler.message(output)) : List.of(),
                assembler.run(r, c.publicId()),
                0,
                false,
                0,
                0,
                0,
                r.contextSnapshotJson());
    }

    private ChatResult cancel(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    ChatRunEntity r = run(command);
                    if (List.of("ACCEPTED", "RUNNING").contains(r.status())) {
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(r.finish("CANCELLED", null))));
                        MessageEntity m =
                                repositories.message.findById(r.assistantMessageId()).entity();
                        Transactions.require(
                                writes.saveMessage(
                                        new MessageAggregate(
                                                m.progress(
                                                        m.content(),
                                                        "CANCELLED",
                                                        m.citationsJson()))));
                    }
                    events.append(
                            account, "run.cancelled", r.publicId(), r.version() + 1, null, "{}");
                    return readRun(command);
                });
    }

    private ChatResult retry(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    ChatRunEntity r = run(command);
                    ConversationEntity c =
                            repositories.conversation.findById(r.conversationId()).entity();
                    if (!active(c).isEmpty()
                            || !repositories
                                    .chatRun
                                    .query(
                                            QueryValue.all("id", 1)
                                                    .where("user_id", "EQ", command.userId())
                                                    .where(
                                                            "status",
                                                            "IN",
                                                            List.of("ACCEPTED", "RUNNING")))
                                    .isEmpty()) {
                        throw new DomainException(DomainErrorCode.BUSY);
                    }
                    MessageEntity input = repositories.message.findById(r.userMessageId()).entity();
                    MessageEntity output =
                            repositories.message.findById(r.assistantMessageId()).entity();
                    if (input.deletedAt() != null
                            || output.deletedAt() != null
                            || !"USER".equals(input.role())
                            || !"ASSISTANT".equals(output.role())) {
                        throw new DomainException(DomainErrorCode.NOT_FOUND);
                    }
                    ChatRunEntity next = r.retry(c.memoryEpoch());
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(next)));
                    Transactions.require(
                            writes.saveMessage(
                                    new MessageAggregate(output.progress("", "ACCEPTED", null))));
                    events.append(
                            account, "run.retried", r.publicId(), r.version() + 1, null, "{}");
                    events.outbox(
                            account.id(),
                            "GENERATE",
                            r.publicId() + ":" + next.attemptCount(),
                            Json.encode(Map.of("runId", r.publicId())));
                    return readRun(command);
                });
    }

    private ChatResult erase(ChatCommand command, boolean entire) {
        transactions.mutate(
                command.userId(),
                account -> {
                    ConversationEntity c = owned(account.id(), command.conversationId());
                    version(c, command.expectedVersion());
                    var running = active(c);
                    if (!entire && !running.isEmpty()) {
                        throw new DomainException(DomainErrorCode.BUSY);
                    }
                    for (var r : running) {
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(
                                                r.entity()
                                                        .finish(
                                                                "CANCELLED",
                                                                "CONVERSATION_DELETED"))));
                    }
                    if (!entire) {
                        if (command.messageIds() == null
                                || command.messageIds().isEmpty()
                                || command.messageIds().size() > 100) {
                            throw new DomainException(DomainErrorCode.INVALID);
                        }
                        var rows =
                                repositories.message.query(
                                        QueryValue.all("id", 100)
                                                .where("user_id", "EQ", account.id())
                                                .where("conversation_id", "EQ", c.id())
                                                .where("public_id", "IN", command.messageIds())
                                                .where("deleted_at", "NULL", null));
                        if (rows.size() != new java.util.HashSet<>(command.messageIds()).size()) {
                            throw new DomainException(DomainErrorCode.NOT_FOUND);
                        }
                        for (var m : rows) {
                            Transactions.require(
                                    writes.saveMessage(new MessageAggregate(m.entity().erase())));
                        }
                    }
                    Transactions.require(
                            writes.saveConversation(
                                    new ConversationAggregate(c.eraseHistory(entire))));
                    events.append(
                            account,
                            entire ? "conversation.deleted" : "messages.deleted",
                            c.publicId(),
                            c.version() + 1,
                            null,
                            "{}");
                    events.outbox(
                            account.id(),
                            "MEMORY_REBUILD",
                            c.publicId() + ":" + (c.memoryEpoch() + 1),
                            Json.encode(Map.of("conversationId", c.id())));
                    return null;
                });
        return result(List.of(), List.of(), null, 0, false, 0, 0, 0, null);
    }

    private ChatResult context(ChatCommand command) {
        ConversationEntity c = owned(command.userId(), command.conversationId());
        var found =
                repositories.chatRun.query(
                        QueryValue.all("id", 1).desc().where("conversation_id", "EQ", c.id()));
        String data =
                found.isEmpty()
                        ? Json.encode(
                                Map.of(
                                        "window",
                                        32768,
                                        "inputEstimate",
                                        0,
                                        "outputReserve",
                                        4096,
                                        "safetyReserve",
                                        4096,
                                        "estimator",
                                        "utf8-upper-v1",
                                        "compression",
                                        "IDLE"))
                        : found.get(0).entity().contextSnapshotJson();
        return result(
                List.of(),
                List.of(),
                found.isEmpty() ? null : assembler.run(found.get(0).entity(), c.publicId()),
                0,
                false,
                0,
                0,
                0,
                data);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
