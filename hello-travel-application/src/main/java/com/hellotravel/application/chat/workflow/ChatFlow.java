package com.hellotravel.application.chat.workflow;

import com.hellotravel.application.chat.assembler.ChatApplicationAssembler;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.memory.assembler.ContextApplicationAssembler;
import com.hellotravel.application.model.policy.ModelContextPolicy;
import com.hellotravel.application.persistence.DomainWrites;
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
    private final DomainWrites writes;
    private final Transactions transactions;
    private final SyncEvents events;
    private final ModelContextPolicy contextPolicy;
    private final ChatApplicationAssembler assembler;
    private final ContextApplicationAssembler contextApplicationAssembler;

    public ChatFlow(
            DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events,
            ChatApplicationAssembler assembler,
            ModelContextPolicy contextPolicy,
            ContextApplicationAssembler contextApplicationAssembler) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
        this.assembler = assembler;
        this.contextPolicy = contextPolicy;
        this.contextApplicationAssembler = contextApplicationAssembler;
    }

    /**
     * 分发可信业务动作。
     *
     * @author AIGenerator
     * @param command 受控command参数
     * @return 当前操作的业务结果
     */
    public ChatResult perform(ChatCommand command) {
        // 1. 分页数量限定在1到200，防止全量历史被无界单次查询。
        if (command.limit() < 1 || command.limit() > 200) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 返回本段实际处理结果，保持本层输出契约。
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
            default -> throw new ApplicationException(ApplicationErrorCode.INVALID);
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
        // 1. 读取对话，限定当前用户及查询窗口。
        var found =
                repositories.conversation.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", userId)
                                .where("public_id", "EQ", publicId)
                                .where("deleted_at", "NULL", null));
        // 2. 对象不存在时按当前用例的NOT_FOUND契约拒绝操作。
        if (found.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
        }
        // 3. 返回当前用户可访问的对象，已删除或越权对象不会进入后续操作。
        return found.get(0).entity();
    }

    private ChatRunEntity run(ChatCommand command) {
        // 1. 读取生成任务，限定当前用户及查询窗口。
        var found =
                repositories.chatRun.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", command.userId())
                                .where("public_id", "EQ", command.runId()));
        // 2. 对象不存在时按当前用例的NOT_FOUND契约拒绝操作。
        if (found.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
        }
        // 3. 按可信内部标识读取对话当前快照。
        ConversationEntity conversation =
                repositories.conversation.findById(found.get(0).entity().conversationId()).entity();
        // 4. 核对删除状态与当前记忆代次，失败中止当前处理。
        if (conversation.deletedAt() != null) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
        }
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return found.get(0).entity();
    }

    private void version(ConversationEntity c, Long expected) {
        // 1. 核对客户端预期版本，防止旧页面覆盖较新对话状态。
        if (expected == null || !c.version().equals(expected)) {
            throw new ApplicationException(ApplicationErrorCode.CONFLICT);
        }
    }

    private List<ChatRunAggregate> active(ConversationEntity c) {
        return repositories.chatRun.query(
                QueryValue.all("id", 1)
                        .where("conversation_id", "EQ", c.id())
                        .where("status", "IN", List.of("ACCEPTED", "RUNNING")));
    }

    private ChatResult bootstrap(ChatCommand command) {
        return transactions.snapshot(
                () -> {
                    // 1. 读取对话，限定当前用户及查询窗口。
                    var newest =
                            repositories.conversation.query(
                                    QueryValue.all("id", 1)
                                            .desc()
                                            .where("user_id", "EQ", command.userId()));
                    long max = newest.isEmpty() ? 0 : newest.get(0).entity().id();
                    long sync =
                            repositories.userAccount.findById(command.userId()).entity().syncSeq();
                    // 2. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return assembler.bootstrap(max, sync);
                });
    }

    private ChatResult list(ChatCommand command) {
        // 1. 组装受控查询条件与分页边界。
        QueryValue query =
                QueryValue.all("id", command.limit())
                        .where("user_id", "EQ", command.userId())
                        .where("deleted_at", "NULL", null)
                        .where("id", "GT", command.after());
        // 2. 核对分页游标与快照上界，防止越界或无法推进的恢复。
        if (command.maxSeq() != null) {
            query = query.where("id", "LE", command.maxSeq());
        }
        // 3. 读取对话，按当前用例条件限定查询窗口。
        var rows = repositories.conversation.query(query);
        // 4. 返回本段实际处理结果，保持本层输出契约。
        return assembler.tabs(rows, command);
    }

    private ChatResult create(ChatCommand command) {
        // 1. 取得原始或默认标题，由领域工厂统一校验和去除首尾空白。
        String name = command.title() == null ? "新对话" : command.title();
        // 2. 进入短事务完成原子变更，外层统一负责提交、回滚与失败转换。
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    ConversationEntity created =
                            ConversationAggregate.started(
                                            account.id(),
                                            name,
                                            now(),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                                    .entity();
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            writes.saveConversation(new ConversationAggregate(created)));
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account, "conversation.created", created.publicId(), 0, null, "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return assembler.changed(created, account);
                });
    }

    private ChatResult rename(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得事务内重新加载的持久化快照，供本段后续处理使用。
                    ConversationEntity stored = owned(account.id(), command.conversationId());
                    // 2. 执行version职责步骤，并把失败交给所属事务或入口处理。
                    version(stored, command.expectedVersion());
                    // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            writes.saveConversation(
                                    new ConversationAggregate(stored).rename(command.title())));
                    // 4. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "conversation.renamed",
                            stored.publicId(),
                            stored.version() + 1,
                            null,
                            "{}");
                    // 5. 读取当前持久化快照，避免依据外部旧快照直接写入。
                    ConversationEntity current = owned(account.id(), stored.publicId());
                    // 6. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return assembler.changed(current, account);
                });
    }

    private ChatResult history(ChatCommand command) {
        // 1. 取得当前对话或验证码快照，供本段后续处理使用。
        ConversationEntity c = owned(command.userId(), command.conversationId());
        // 2. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
        if (command.historyEpoch() != null && !c.historyEpoch().equals(command.historyEpoch())) {
            throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
        }
        // 3. 取得本次操作的容量上限，供本段后续处理使用。
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
        // 4. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
        if (!repositories
                .conversation
                .findById(c.id())
                .entity()
                .historyEpoch()
                .equals(c.historyEpoch())) {
            throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
        }
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return assembler.history(c, rows, command, max);
    }

    private synchronized ChatResult submit(ChatCommand command) {
        // 1. 核对输入或读取结果的存在性，失败中止当前处理。
        if (command.text() == null || command.text().isBlank() || command.text().length() > 8000) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try {
            UUID.fromString(command.requestKey());
        } catch (RuntimeException e) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 3. 计算请求正文摘要，绑定幂等键与实际内容。
        byte[] digest = Ids.hash(command.text());
        // 4. 进入短事务完成原子变更，外层统一负责提交、回滚与失败转换。
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得当前对话或验证码快照，供本段后续处理使用。
                    ConversationEntity c = owned(account.id(), command.conversationId());
                    var replay =
                            repositories.chatRun.query(
                                    QueryValue.all("id", 1)
                                            .where("user_id", "EQ", account.id())
                                            .where("conversation_id", "EQ", c.id())
                                            .where("request_key", "EQ", command.requestKey()));
                    // 2. 核对输入或读取结果的存在性，失败中止当前处理。
                    if (!replay.isEmpty()) {
                        if (!java.security.MessageDigest.isEqual(
                                digest, replay.get(0).entity().requestDigest())) {
                            throw new ApplicationException(
                                    ApplicationErrorCode.IDEMPOTENCY_CONFLICT);
                        }
                        // 即使幂等重放也提交一个无正文事件，保持账号序列没有补齐缺口。
                        events.append(
                                account,
                                "run.replayed",
                                replay.get(0).entity().publicId(),
                                replay.get(0).entity().version(),
                                null,
                                "{}");
                        return readRun(
                                assembler.commandForRun(
                                        command, replay.get(0).entity().publicId()));
                    }
                    // 3. 核对实体当前状态与允许的操作，失败中止当前处理。
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
                        throw new ApplicationException(ApplicationErrorCode.RATE_LIMITED);
                    }
                    // 4. 核对输入或读取结果的存在性，失败中止当前处理。
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
                        throw new ApplicationException(ApplicationErrorCode.BUSY);
                    }
                    // 5. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    MessageEntity input =
                            MessageAggregate.userInput(c, command.text(), now()).entity();
                    MessageEntity output = MessageAggregate.assistantPlaceholder(c, now()).entity();
                    // 6. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            writes.saveConversation(new ConversationAggregate(c).appendPair()));
                    // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(writes.saveMessage(new MessageAggregate(input)));
                    // 8. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(writes.saveMessage(new MessageAggregate(output)));
                    // 9. 读取消息，按当前用例条件限定查询窗口。
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
                            ChatRunAggregate.accepted(
                                            c,
                                            command.sessionId(),
                                            command.requestKey(),
                                            digest,
                                            storedInput,
                                            storedOutput,
                                            now())
                                    .entity();
                    // 10. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(run)));
                    // 11. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "messages.accepted",
                            c.publicId(),
                            c.version() + 1,
                            null,
                            Json.encode(Map.of("runId", run.publicId())));
                    // 12. 同事务登记可恢复后台任务，外部调用在提交之后执行。
                    events.outbox(
                            account.id(),
                            "GENERATE",
                            run.publicId(),
                            Json.encode(Map.of("runId", run.publicId())));
                    // 13. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return assembler.accepted(List.of(input, output), run, c, account);
                });
    }

    private ChatResult readRun(ChatCommand command) {
        // 1. 取得下层返回的标准结果，供本段后续处理使用。
        ChatRunEntity r = run(command);
        ConversationEntity c = repositories.conversation.findById(r.conversationId()).entity();
        MessageEntity output = repositories.message.findById(r.assistantMessageId()).entity();
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return assembler.runSnapshot(r, c, output);
    }

    private ChatResult cancel(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得下层返回的标准结果，供本段后续处理使用。
                    ChatRunEntity r = run(command);
                    // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                    if (List.of("ACCEPTED", "RUNNING").contains(r.status())) {
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(r).finish("CANCELLED", null)));
                        MessageEntity m =
                                repositories.message.findById(r.assistantMessageId()).entity();
                        Transactions.require(
                                writes.saveMessage(
                                        new MessageAggregate(m)
                                                .progress(
                                                        m.content(),
                                                        "CANCELLED",
                                                        m.citationsJson())));
                    }
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account, "run.cancelled", r.publicId(), r.version() + 1, null, "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return readRun(command);
                });
    }

    private ChatResult retry(ChatCommand command) {
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得下层返回的标准结果，供本段后续处理使用。
                    ChatRunEntity r = run(command);
                    ConversationEntity c =
                            repositories.conversation.findById(r.conversationId()).entity();
                    // 2. 核对输入或读取结果的存在性，失败中止当前处理。
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
                        throw new ApplicationException(ApplicationErrorCode.BUSY);
                    }
                    // 3. 按可信内部标识读取消息当前快照。
                    MessageEntity input = repositories.message.findById(r.userMessageId()).entity();
                    MessageEntity output =
                            repositories.message.findById(r.assistantMessageId()).entity();
                    // 4. 核对关联对象归属与角色，拒绝跨账号或跨会话关联。
                    if (input.deletedAt() != null
                            || output.deletedAt() != null
                            || !"USER".equals(input.role())
                            || !"ASSISTANT".equals(output.role())) {
                        throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
                    }
                    // 5. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    ChatRunEntity next = new ChatRunAggregate(r).retry(c.memoryEpoch()).entity();
                    // 6. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(next)));
                    // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            writes.saveMessage(
                                    new MessageAggregate(output).progress("", "ACCEPTED", null)));
                    // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account, "run.retried", r.publicId(), r.version() + 1, null, "{}");
                    // 9. 同事务登记可恢复后台任务，外部调用在提交之后执行。
                    events.outbox(
                            account.id(),
                            "GENERATE",
                            r.publicId() + ":" + next.attemptCount(),
                            Json.encode(Map.of("runId", r.publicId())));
                    // 10. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return readRun(command);
                });
    }

    private ChatResult erase(ChatCommand command, boolean entire) {
        // 1. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得当前对话或验证码快照，供本段后续处理使用。
                    ConversationEntity c = owned(account.id(), command.conversationId());
                    // 2. 执行version职责步骤，并把失败交给所属事务或入口处理。
                    version(c, command.expectedVersion());
                    // 3. 取得当前租约下的运行快照，供本段后续处理使用。
                    var running = active(c);
                    // 4. 核对输入或读取结果的存在性，失败中止当前处理。
                    if (!entire && !running.isEmpty()) {
                        throw new ApplicationException(ApplicationErrorCode.BUSY);
                    }
                    // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var r : running) {
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(r.entity())
                                                .finish("CANCELLED", "CONVERSATION_DELETED")));
                    }
                    // 6. 局部删消息时校验稳定消息标识，全量清理走独立代次处理。
                    if (!entire) {
                        if (command.messageIds() == null
                                || command.messageIds().isEmpty()
                                || command.messageIds().size() > 100) {
                            throw new ApplicationException(ApplicationErrorCode.INVALID);
                        }
                        var rows =
                                repositories.message.query(
                                        QueryValue.all("id", 100)
                                                .where("user_id", "EQ", account.id())
                                                .where("conversation_id", "EQ", c.id())
                                                .where("public_id", "IN", command.messageIds())
                                                .where("deleted_at", "NULL", null));
                        if (rows.size() != new java.util.HashSet<>(command.messageIds()).size()) {
                            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
                        }
                        for (var m : rows) {
                            Transactions.require(
                                    writes.saveMessage(new MessageAggregate(m.entity()).erase()));
                        }
                    }
                    // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            writes.saveConversation(
                                    new ConversationAggregate(c).eraseHistory(entire)));
                    // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            entire ? "conversation.deleted" : "messages.deleted",
                            c.publicId(),
                            c.version() + 1,
                            null,
                            "{}");
                    // 9. 同事务登记可恢复后台任务，外部调用在提交之后执行。
                    events.outbox(
                            account.id(),
                            "MEMORY_REBUILD",
                            c.publicId() + ":" + (c.memoryEpoch() + 1),
                            Json.encode(Map.of("conversationId", c.id())));
                    // 10. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return assembler.completed();
    }

    private ChatResult context(ChatCommand command) {
        // 1. 取得当前对话或验证码快照，供本段后续处理使用。
        ConversationEntity c = owned(command.userId(), command.conversationId());
        var found =
                repositories.chatRun.query(
                        QueryValue.all("id", 1).desc().where("conversation_id", "EQ", c.id()));
        String data =
                found.isEmpty()
                        ? contextApplicationAssembler.initial(contextPolicy)
                        : found.get(0).entity().contextSnapshotJson();
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return assembler.context(c, found, data);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
