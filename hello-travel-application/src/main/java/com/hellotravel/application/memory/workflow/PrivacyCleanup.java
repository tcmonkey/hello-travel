package com.hellotravel.application.memory.workflow;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.model.entity.MemoryFactEntity;
import com.hellotravel.domain.memory.model.entity.MemorySummaryEntity;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 删除代次先使记忆失效，再分批清除派生正文与证据，不恢复已删除对话。
 *
 * @author AIGenerator
 */
@Component
public final class PrivacyCleanup {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    public PrivacyCleanup(
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
     * 执行后台任务并保留租约与代次保护。
     *
     * @author AIGenerator
     * @param conversationId 受控conversationId参数
     */
    public void execute(Long conversationId) {
        var stored = repositories.conversation.findById(conversationId);
        if (stored == null) {
            return;
        }
        var conversation = stored.entity();
        transactions.mutate(
                conversation.userId(),
                account -> {
                    var c = repositories.conversation.findById(conversationId).entity();
                    for (var row :
                            repositories.memorySummary.query(
                                    QueryValue.all("id", 500)
                                            .where("conversation_id", "EQ", c.id())
                                            .where("memory_epoch", "LT", c.memoryEpoch())
                                            .where("status", "EQ", "ACTIVE"))) {
                        var old = row.entity();
                        var next =
                                new MemorySummaryEntity(
                                        old.id(),
                                        old.publicId(),
                                        old.userId(),
                                        old.conversationId(),
                                        old.memoryEpoch(),
                                        old.coveredFromSeq(),
                                        old.coveredThroughSeq(),
                                        "{}",
                                        old.estimatedTokens(),
                                        old.estimatorVersion(),
                                        old.modelName(),
                                        old.promptRevision(),
                                        "INVALIDATED",
                                        old.createdAt(),
                                        old.updatedAt(),
                                        old.version());
                        Transactions.require(
                                writes.saveMemorySummary(new MemorySummaryAggregate(next)));
                    }
                    for (var row :
                            repositories.memoryFact.query(
                                    QueryValue.all("id", 500)
                                            .where("conversation_id", "EQ", c.id())
                                            .where("memory_epoch", "LT", c.memoryEpoch())
                                            .where("status", "EQ", "ACTIVE"))) {
                        var old = row.entity();
                        for (var source :
                                repositories.memoryFactSource.query(
                                        QueryValue.all("id", 1000)
                                                .where("fact_id", "EQ", old.id()))) {
                            Transactions.require(
                                    writes.removeMemoryFactSource(source.entity().id()));
                        }
                        var next =
                                new MemoryFactEntity(
                                        old.id(),
                                        old.publicId(),
                                        old.userId(),
                                        old.conversationId(),
                                        old.memoryEpoch(),
                                        old.factKey(),
                                        old.category(),
                                        "",
                                        old.evidenceType(),
                                        0,
                                        "INVALIDATED",
                                        old.expiresAt(),
                                        old.createdAt(),
                                        old.updatedAt(),
                                        old.version());
                        Transactions.require(writes.saveMemoryFact(new MemoryFactAggregate(next)));
                    }
                    if (c.deletedAt() != null) {
                        for (var row :
                                repositories.message.query(
                                        QueryValue.all("id", 500)
                                                .where("conversation_id", "EQ", c.id())
                                                .where("deleted_at", "NULL", null))) {
                            Transactions.require(
                                    writes.saveMessage(new MessageAggregate(row.entity().erase())));
                        }
                    }
                    boolean remaining =
                            !repositories
                                            .memoryFact
                                            .query(
                                                    QueryValue.all("id", 1)
                                                            .where("conversation_id", "EQ", c.id())
                                                            .where(
                                                                    "memory_epoch",
                                                                    "LT",
                                                                    c.memoryEpoch())
                                                            .where("status", "EQ", "ACTIVE"))
                                            .isEmpty()
                                    || !repositories
                                            .memorySummary
                                            .query(
                                                    QueryValue.all("id", 1)
                                                            .where("conversation_id", "EQ", c.id())
                                                            .where(
                                                                    "memory_epoch",
                                                                    "LT",
                                                                    c.memoryEpoch())
                                                            .where("status", "EQ", "ACTIVE"))
                                            .isEmpty()
                                    || (c.deletedAt() != null
                                            && !repositories
                                                    .message
                                                    .query(
                                                            QueryValue.all("id", 1)
                                                                    .where(
                                                                            "conversation_id",
                                                                            "EQ",
                                                                            c.id())
                                                                    .where(
                                                                            "deleted_at",
                                                                            "NULL",
                                                                            null))
                                                    .isEmpty());
                    if (remaining) {
                        events.outbox(
                                c.userId(),
                                "MEMORY_REBUILD",
                                c.publicId() + ":" + c.memoryEpoch() + ":" + account.syncSeq(),
                                Json.encode(Map.of("conversationId", c.id())));
                    }
                    events.append(account, "memory.cleaned", c.publicId(), c.version(), null, "{}");
                    return null;
                });
    }
}
