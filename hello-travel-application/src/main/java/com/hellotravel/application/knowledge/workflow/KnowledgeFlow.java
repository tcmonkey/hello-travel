package com.hellotravel.application.knowledge.workflow;

import com.hellotravel.application.file.adaptor.FileOutAdaptor;
import com.hellotravel.application.file.command.FileCommand;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.DocumentResult;
import com.hellotravel.application.knowledge.result.KnowledgeResult;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MySQL保存原文和状态，Milvus仅保存隔离向量；删除先阻断检索再异步清理。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeFlow {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    private final FileOutAdaptor files;

    public KnowledgeFlow(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events,
            FileOutAdaptor files) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
        this.files = files;
    }

    /**
     * 读取指定账号拥有的未删除对象。
     *
     * @author AIGenerator
     * @param userId 认证账号主键
     * @param id 可信内部主键
     * @return 归属和状态校验后的业务快照
     */
    public KnowledgeDocumentEntity owned(Long userId, String id) {
        var rows =
                repositories.knowledgeDocument.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", userId)
                                .where("public_id", "EQ", id)
                                .where("deleted_at", "NULL", null));
        if (rows.isEmpty()) {
            throw new DomainException(DomainErrorCode.NOT_FOUND);
        }
        return rows.get(0).entity();
    }

    /**
     * 分发可信业务动作。
     *
     * @author AIGenerator
     * @param command 受控command参数
     * @return 当前操作的业务结果
     */
    public KnowledgeResult perform(KnowledgeCommand command) {
        if ("LIST".equals(command.action())) {
            int limit = Math.min(100, Math.max(1, command.limit()));
            var rows =
                    repositories.knowledgeDocument.query(
                            QueryValue.all("id", limit)
                                    .where("user_id", "EQ", command.userId())
                                    .where("deleted_at", "NULL", null)
                                    .where("id", "GT", command.after()));
            return new KnowledgeResult(
                    rows.stream().map(x -> view(x.entity(), false)).toList(),
                    rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().id(),
                    rows.size() == limit);
        }
        if ("READ".equals(command.action())) {
            return new KnowledgeResult(
                    List.of(view(owned(command.userId(), command.documentId()), true)), 0, false);
        }
        if ("UPLOAD".equals(command.action())) {
            if (command.sourceUrl() != null && !command.sourceUrl().isBlank()) {
                java.net.URI uri = java.net.URI.create(command.sourceUrl());
                if (command.sourceUrl().length() > 2048
                        || !"https".equals(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) {
                    throw new DomainException(DomainErrorCode.INVALID);
                }
            }
            var result =
                    files.store(new FileCommand("SAVE", command.filename(), command.bytes(), null));
            if (!result.success()) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            var parsed = result.data();
            try {
                return transactions.mutate(
                        command.userId(),
                        account -> {
                            var document =
                                    new KnowledgeDocumentEntity(
                                            null,
                                            Ids.next(),
                                            command.userId(),
                                            command.filename()
                                                    .substring(
                                                            0,
                                                            Math.min(
                                                                    200,
                                                                    command.filename().length())),
                                            command.filename(),
                                            parsed.mime(),
                                            parsed.storageKey(),
                                            (long) command.bytes().length,
                                            parsed.hash(),
                                            parsed.text(),
                                            command.sourceUrl(),
                                            null,
                                            null,
                                            "RECEIVED",
                                            1L,
                                            "text-embedding-v4",
                                            1024,
                                            "hello_travel_kb_v1_d1024",
                                            null,
                                            null,
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                            0L);
                            Transactions.require(
                                    writes.saveKnowledgeDocument(
                                            new KnowledgeDocumentAggregate(document)));
                            document = owned(command.userId(), document.publicId());
                            job(document, "INGEST");
                            events.append(
                                    account,
                                    "knowledge.created",
                                    document.publicId(),
                                    document.version(),
                                    null,
                                    "{}");
                            return new KnowledgeResult(List.of(view(document, true)), 0, false);
                        });
            } catch (RuntimeException exception) {
                files.store(new FileCommand("DELETE", null, null, parsed.storageKey()));
                throw exception;
            }
        }
        return transactions.mutate(
                command.userId(),
                account -> {
                    var document = owned(command.userId(), command.documentId());
                    if (command.expectedVersion() == null
                            || !command.expectedVersion().equals(document.version())) {
                        throw new DomainException(DomainErrorCode.CONFLICT);
                    }
                    if ("RETRY".equals(command.action())) {
                        if (!"FAILED".equals(document.status())) {
                            throw new DomainException(DomainErrorCode.CONFLICT);
                        }
                        document = document.reindex();
                        Transactions.require(
                                writes.saveKnowledgeDocument(
                                        new KnowledgeDocumentAggregate(document)));
                        job(document, "INGEST");
                    } else if ("DELETE".equals(command.action())) {
                        document = document.erase();
                        Transactions.require(
                                writes.saveKnowledgeDocument(
                                        new KnowledgeDocumentAggregate(document)));
                        job(document, "DELETE");
                    } else {
                        throw new DomainException(DomainErrorCode.INVALID);
                    }
                    events.append(
                            account,
                            "knowledge.changed",
                            document.publicId(),
                            document.version() + 1,
                            null,
                            "{}");
                    return new KnowledgeResult(List.of(view(document, false)), 0, false);
                });
    }

    private void job(KnowledgeDocumentEntity document, String type) {
        IndexJobEntity job =
                new IndexJobEntity(
                        null,
                        Ids.next(),
                        document.userId(),
                        document.id(),
                        document.indexGeneration(),
                        type,
                        "PENDING",
                        0,
                        1,
                        null,
                        null,
                        0L,
                        null,
                        null,
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                        0L);
        Transactions.require(writes.saveIndexJob(new IndexJobAggregate(job)));
    }

    private DocumentResult view(KnowledgeDocumentEntity entity, boolean body) {
        return new DocumentResult(
                entity.publicId(),
                entity.title(),
                entity.status(),
                entity.version(),
                entity.sourceUrl(),
                entity.errorCode(),
                body
                        ? entity.extractedText()
                        : entity.extractedText() == null
                                ? null
                                : entity.extractedText()
                                        .substring(
                                                0, Math.min(240, entity.extractedText().length())));
    }
}
