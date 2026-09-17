package com.hellotravel.application.travel.workflow;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.model.travel.ModelDO;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 生成租约/epoch栅栏、调用证据和回答落库，所有SQL事务独立于模型等待。
 *
 * @author AIGenerator
 */
@Component
public final class RunCoordinator {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    public RunCoordinator(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
    }

    /**
     * 领取待执行任务并提高租约栅栏。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public ChatRunEntity claim(String id) {
        var rows = repositories.chatRun.query(QueryValue.all("id", 1).where("public_id", "EQ", id));
        if (rows.isEmpty()) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        ChatRunEntity selected = rows.get(0).entity();
        return transactions.mutate(
                selected.userId(),
                account -> {
                    ChatRunEntity current = repositories.chatRun.findById(selected.id()).entity();
                    ConversationEntity c =
                            repositories.conversation.findById(current.conversationId()).entity();
                    if (c.deletedAt() != null
                            || !c.memoryEpoch().equals(current.memoryEpochAtStart())) {
                        throw new DomainException(DomainErrorCode.CONFLICT);
                    }
                    if (!"ACCEPTED".equals(current.status())) {
                        throw new DomainException(DomainErrorCode.CONFLICT);
                    }
                    ChatRunEntity next = current.claim(Ids.next());
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(next)));
                    MessageEntity output =
                            repositories.message.findById(next.assistantMessageId()).entity();
                    Transactions.require(
                            writes.saveMessage(
                                    new MessageAggregate(
                                            output.progress(output.content(), "STREAMING", null))));
                    events.append(
                            account,
                            "run.started",
                            next.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    return repositories.chatRun.findById(current.id()).entity();
                });
    }

    /**
     * 验证运行栅栏及对话记忆代次，阻止过期输出。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @return 当前操作的业务结果
     */
    public ChatRunEntity requireCurrent(ChatRunEntity expected) {
        ChatRunEntity current = repositories.chatRun.findById(expected.id()).entity();
        ConversationEntity c =
                repositories.conversation.findById(expected.conversationId()).entity();
        if (current.leaseUntil() == null
                || current.leaseUntil().isBefore(now())
                || !"RUNNING".equals(current.status())
                || !current.leaseFence().equals(expected.leaseFence())
                || !current.attemptCount().equals(expected.attemptCount())
                || c.deletedAt() != null
                || !c.memoryEpoch().equals(expected.memoryEpochAtStart())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
        return current;
    }

    /**
     * 保存当前步骤与有界上下文预算。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param node 当前工作流步骤
     * @param budget 有界上下文预算JSON
     */
    public void checkpoint(ChatRunEntity expected, String node, String budget) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    ChatRunEntity current = requireCurrent(expected);
                    Transactions.require(
                            writes.saveChatRun(
                                    new ChatRunAggregate(
                                            current.checkpoint(
                                                    node,
                                                    Json.encode(
                                                            Map.of(
                                                                    "revision",
                                                                    "travel-v1",
                                                                    "node",
                                                                    node)),
                                                    budget))));
                    events.append(
                            account,
                            "context.updated",
                            expected.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    return null;
                });
    }

    /**
     * 更新助手正文与状态，拒绝向已删除消息写入。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param text 有界文本内容
     * @return 当前操作的业务结果
     */
    public boolean progress(ChatRunEntity expected, String text) {
        try {
            transactions.mutate(
                    expected.userId(),
                    account -> {
                        ChatRunEntity current = requireCurrent(expected);
                        // 每次草稿落库同时续租；lease仅由相同fence持有者推进。
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(
                                                current.checkpoint(
                                                        current.graphNode(),
                                                        current.stateJson(),
                                                        current.contextSnapshotJson()))));
                        MessageEntity output =
                                repositories
                                        .message
                                        .findById(expected.assistantMessageId())
                                        .entity();
                        Transactions.require(
                                writes.saveMessage(
                                        new MessageAggregate(
                                                output.progress(text, "STREAMING", null))));
                        ConversationEntity c =
                                repositories
                                        .conversation
                                        .findById(expected.conversationId())
                                        .entity();
                        events.append(
                                account,
                                "message.progress",
                                c.publicId(),
                                output.version() + 1,
                                null,
                                Json.encode(
                                        Map.of(
                                                "messageId",
                                                output.publicId(),
                                                "runId",
                                                expected.publicId())));
                        return null;
                    });
            return true;
        } catch (DomainException exception) {
            return false;
        }
    }

    /**
     * 在模型调用前保存尝试证据。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param stage 受控stage参数
     * @param call 本尝试内的压缩调用编号
     * @param estimated 受控estimated参数
     * @return 当前操作的业务结果
     */
    public String invocation(ChatRunEntity expected, String stage, int call, int estimated) {
        return transactions.plain(
                () -> {
                    requireCurrent(expected);
                    ModelInvocationEntity created =
                            new ModelInvocationEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    expected.userId(),
                                    expected.conversationId(),
                                    expected.id(),
                                    stage,
                                    expected.attemptCount(),
                                    call,
                                    "configured-chat",
                                    "travel-v1",
                                    estimated,
                                    "utf8-upper-v1",
                                    null,
                                    null,
                                    null,
                                    null,
                                    "STARTED",
                                    null,
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(
                            writes.saveModelInvocation(new ModelInvocationAggregate(created)));
                    return created.publicId();
                });
    }

    /**
     * 记录供应商实际返回的用量和调用结果。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @param response HTTP响应
     * @param latency 受控latency参数
     * @param success 受控success参数
     */
    public void invocationComplete(String id, ModelDO response, long latency, boolean success) {
        transactions.plain(
                () -> {
                    var current =
                            repositories
                                    .modelInvocation
                                    .query(QueryValue.all("id", 1).where("public_id", "EQ", id))
                                    .get(0)
                                    .entity();
                    ModelInvocationEntity next =
                            new ModelInvocationEntity(
                                    current.id(),
                                    current.publicId(),
                                    current.userId(),
                                    current.conversationId(),
                                    current.runId(),
                                    current.stage(),
                                    current.runAttemptNo(),
                                    current.attemptNo(),
                                    response == null ? current.modelName() : response.model(),
                                    current.promptRevision(),
                                    current.estimatedInputTokens(),
                                    current.estimatorVersion(),
                                    response == null ? null : response.inputTokens(),
                                    response == null ? null : response.outputTokens(),
                                    (int) Math.min(Integer.MAX_VALUE, latency),
                                    response == null ? null : response.providerId(),
                                    success ? "SUCCEEDED" : "FAILED",
                                    success ? null : "MODEL_UNAVAILABLE",
                                    now(),
                                    current.createdAt(),
                                    current.updatedAt(),
                                    current.version());
                    Transactions.require(
                            writes.saveModelInvocation(new ModelInvocationAggregate(next)));
                    return null;
                });
    }

    /**
     * 原子保存完整回答、任务终态和记忆提取任务。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param text 有界文本内容
     * @param citations 经校验的来源引用
     * @param response HTTP响应
     * @param budget 有界上下文预算JSON
     */
    public void finalizeAnswer(
            ChatRunEntity expected,
            String text,
            String citations,
            ModelDO response,
            String budget) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    ChatRunEntity current = requireCurrent(expected);
                    ChatRunEntity finished =
                            current.checkpoint("finalize", current.stateJson(), budget)
                                    .finish("COMPLETED", null);
                    Transactions.require(writes.saveChatRun(new ChatRunAggregate(finished)));
                    MessageEntity output =
                            repositories.message.findById(expected.assistantMessageId()).entity();
                    Transactions.require(
                            writes.saveMessage(
                                    new MessageAggregate(
                                            output.progress(text, "COMPLETED", citations))));
                    ConversationEntity c =
                            repositories.conversation.findById(expected.conversationId()).entity();
                    events.append(
                            account,
                            "message.completed",
                            c.publicId(),
                            output.version() + 1,
                            null,
                            Json.encode(Map.of("runId", expected.publicId())));
                    events.outbox(
                            account.id(),
                            "MEMORY_EXTRACT",
                            expected.publicId() + ":" + expected.attemptCount(),
                            Json.encode(Map.of("runId", expected.publicId())));
                    return null;
                });
    }

    /**
     * 保留回答草稿并记录当前尝试失败。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param error 待分类的失败
     */
    public void fail(ChatRunEntity expected, String error) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    ChatRunEntity current = repositories.chatRun.findById(expected.id()).entity();
                    if ("RUNNING".equals(current.status())
                            && current.leaseFence().equals(expected.leaseFence())) {
                        Transactions.require(
                                writes.saveChatRun(
                                        new ChatRunAggregate(current.finish("FAILED", error))));
                        MessageEntity output =
                                repositories
                                        .message
                                        .findById(current.assistantMessageId())
                                        .entity();
                        Transactions.require(
                                writes.saveMessage(
                                        new MessageAggregate(
                                                output.progress(
                                                        output.content(),
                                                        "FAILED",
                                                        output.citationsJson()))));
                    }
                    events.append(
                            account,
                            "run.failed",
                            current.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    return null;
                });
    }

    /**
     * 将租约过期任务标为中断，等待用户显式重试。
     *
     * @author AIGenerator
     */
    public void recoverExpired() {
        for (var row :
                repositories.chatRun.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "RUNNING")
                                .where("lease_until", "LT", now()))) {
            transactions.mutate(
                    row.entity().userId(),
                    account -> {
                        ChatRunEntity current =
                                repositories.chatRun.findById(row.entity().id()).entity();
                        if ("RUNNING".equals(current.status())
                                && current.leaseUntil().isBefore(now())) {
                            Transactions.require(
                                    writes.saveChatRun(
                                            new ChatRunAggregate(
                                                    current.finish("INTERRUPTED", "LEASE_LOST"))));
                            MessageEntity output =
                                    repositories
                                            .message
                                            .findById(current.assistantMessageId())
                                            .entity();
                            Transactions.require(
                                    writes.saveMessage(
                                            new MessageAggregate(
                                                    output.progress(
                                                            output.content(),
                                                            "INTERRUPTED",
                                                            output.citationsJson()))));
                        }
                        events.append(
                                account,
                                "run.interrupted",
                                current.publicId(),
                                current.version() + 1,
                                null,
                                "{}");
                        return null;
                    });
        }
    }

    /**
     * 将发件箱未知领取后的待执行任务标为中断，用户可显式重试。
     *
     * @author AIGenerator
     */
    public void recoverAccepted() {
        for (var row :
                repositories.chatRun.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "ACCEPTED")
                                .where("updated_at", "LT", now().minusMinutes(15)))) {
            var selected = row.entity();
            transactions.mutate(
                    selected.userId(),
                    account -> {
                        var current = repositories.chatRun.findById(selected.id()).entity();
                        if ("ACCEPTED".equals(current.status())
                                && current.updatedAt().isBefore(now().minusMinutes(15))) {
                            Transactions.require(
                                    writes.saveChatRun(
                                            new ChatRunAggregate(
                                                    current.finish(
                                                            "INTERRUPTED", "DISPATCH_UNKNOWN"))));
                            var answer =
                                    repositories
                                            .message
                                            .findById(current.assistantMessageId())
                                            .entity();
                            if (answer.deletedAt() == null) {
                                Transactions.require(
                                        writes.saveMessage(
                                                new MessageAggregate(
                                                        answer.progress(
                                                                answer.content(),
                                                                "INTERRUPTED",
                                                                answer.citationsJson()))));
                            }
                        }
                        events.append(
                                account,
                                "run.interrupted",
                                current.publicId(),
                                current.version() + 1,
                                null,
                                "{}");
                        return null;
                    });
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
