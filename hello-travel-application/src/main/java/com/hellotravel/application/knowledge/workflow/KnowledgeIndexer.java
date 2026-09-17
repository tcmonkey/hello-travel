package com.hellotravel.application.knowledge.workflow;

import com.hellotravel.application.file.adaptor.FileOutAdaptor;
import com.hellotravel.application.file.command.FileCommand;
import com.hellotravel.application.knowledge.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.application.knowledge.command.VectorItemCommand;
import com.hellotravel.application.model.adaptor.ModelOutAdaptor;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeChunkEntity;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 显式代次和租约保护的分块索引；不自动重试未知计费结果。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeIndexer {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    private final ModelOutAdaptor model;

    private final VectorOutAdaptor vectors;

    private final FileOutAdaptor files;

    /**
     * 私有文件分批扫描游标。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.atomic.AtomicReference<String> fileCursor =
            new java.util.concurrent.atomic.AtomicReference<>("");

    /**
     * 保存reconcileCursor对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.atomic.AtomicLong reconcileCursor =
            new java.util.concurrent.atomic.AtomicLong();

    public KnowledgeIndexer(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events,
            ModelOutAdaptor model,
            VectorOutAdaptor vectors,
            FileOutAdaptor files) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
        this.model = model;
        this.vectors = vectors;
        this.files = files;
    }

    /**
     * 执行后台任务并保留租约与代次保护。
     *
     * @author AIGenerator
     * @param jobId 可信内部主键
     */
    public void execute(Long jobId) {
        IndexJobEntity job =
                transactions.plain(
                        () -> {
                            var stored = repositories.indexJob.findById(jobId);
                            if (stored == null || !"PENDING".equals(stored.entity().status())) {
                                return null;
                            }
                            var old = stored.entity();
                            var next =
                                    new IndexJobEntity(
                                            old.id(),
                                            old.publicId(),
                                            old.userId(),
                                            old.documentId(),
                                            old.indexGeneration(),
                                            old.jobType(),
                                            "RUNNING",
                                            old.attemptCount() + 1,
                                            old.maxAttempts(),
                                            old.nextAttemptAt(),
                                            Ids.next(),
                                            old.leaseFence() + 1,
                                            now().plusMinutes(15),
                                            old.errorCode(),
                                            old.createdAt(),
                                            old.updatedAt(),
                                            old.version());
                            Transactions.require(writes.saveIndexJob(new IndexJobAggregate(next)));
                            return repositories.indexJob.findById(jobId).entity();
                        });
        if (job == null) {
            return;
        }
        var document = repositories.knowledgeDocument.findById(job.documentId()).entity();
        try {
            if ("DELETE".equals(job.jobType())) {
                String owner = repositories.userAccount.findById(job.userId()).entity().publicId();
                require(
                        vectors.index(
                                        new VectorCommand(
                                                "DELETE",
                                                owner,
                                                document.publicId(),
                                                document.indexGeneration(),
                                                List.of(),
                                                null))
                                .success());
                require(
                        files.store(new FileCommand("DELETE", null, null, document.storageKey()))
                                .success());
                finish(job, true, null);
                return;
            }
            current(job);
            state(job, "INDEXING", null);
            var text = document.extractedText();
            if (text == null || text.isBlank()) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            List<String> pieces = split(text);
            String owner = repositories.userAccount.findById(job.userId()).entity().publicId();
            for (int offset = 0; offset < pieces.size(); offset += 16) {
                current(job);
                List<String> batch = pieces.subList(offset, Math.min(pieces.size(), offset + 16));
                var embedded =
                        model.generate(new ModelCommand("EMBED", null, List.of(), batch, null));
                require(embedded.success());
                if (embedded.data().vectors() == null
                        || embedded.data().vectors().size() != batch.size()) {
                    throw new DomainException(DomainErrorCode.FAILED);
                }
                List<VectorItemCommand> items = new java.util.ArrayList<>();
                for (int i = 0; i < batch.size(); i++) {
                    int number = offset + i;
                    String body = batch.get(i);
                    byte[] hash = Ids.hash(body);
                    var chunk =
                            transactions.plain(
                                    () -> {
                                        current(job);
                                        var entity =
                                                new KnowledgeChunkEntity(
                                                        null,
                                                        Ids.next(),
                                                        job.userId(),
                                                        job.documentId(),
                                                        job.indexGeneration(),
                                                        number,
                                                        body,
                                                        hash,
                                                        ContextBudgetValue.estimate(body),
                                                        "",
                                                        "PENDING",
                                                        null,
                                                        java.time.LocalDateTime.now(
                                                                java.time.ZoneOffset.UTC),
                                                        java.time.LocalDateTime.now(
                                                                java.time.ZoneOffset.UTC),
                                                        0L);
                                        entity =
                                                new KnowledgeChunkEntity(
                                                        entity.id(),
                                                        entity.publicId(),
                                                        entity.userId(),
                                                        entity.documentId(),
                                                        entity.indexGeneration(),
                                                        entity.chunkNo(),
                                                        entity.content(),
                                                        entity.contentSha256(),
                                                        entity.estimatedTokens(),
                                                        entity.publicId(),
                                                        entity.status(),
                                                        entity.deletedAt(),
                                                        entity.createdAt(),
                                                        entity.updatedAt(),
                                                        entity.version());
                                        Transactions.require(
                                                writes.saveKnowledgeChunk(
                                                        new KnowledgeChunkAggregate(entity)));
                                        return entity;
                                    });
                    items.add(
                            new VectorItemCommand(
                                    chunk.vectorKey(),
                                    document.publicId(),
                                    job.indexGeneration(),
                                    number,
                                    java.util.HexFormat.of().formatHex(hash),
                                    embedded.data().vectors().get(i)));
                }
                current(job);
                require(
                        vectors.index(
                                        new VectorCommand(
                                                "UPSERT",
                                                owner,
                                                document.publicId(),
                                                job.indexGeneration(),
                                                items,
                                                null))
                                .success());
                transactions.plain(
                        () -> {
                            current(job);
                            for (var item : items) {
                                var old =
                                        repositories
                                                .knowledgeChunk
                                                .query(
                                                        QueryValue.all("id", 1)
                                                                .where(
                                                                        "vector_key",
                                                                        "EQ",
                                                                        item.key()))
                                                .get(0)
                                                .entity();
                                var ready =
                                        new KnowledgeChunkEntity(
                                                old.id(),
                                                old.publicId(),
                                                old.userId(),
                                                old.documentId(),
                                                old.indexGeneration(),
                                                old.chunkNo(),
                                                old.content(),
                                                old.contentSha256(),
                                                old.estimatedTokens(),
                                                old.vectorKey(),
                                                "READY",
                                                old.deletedAt(),
                                                old.createdAt(),
                                                old.updatedAt(),
                                                old.version());
                                Transactions.require(
                                        writes.saveKnowledgeChunk(
                                                new KnowledgeChunkAggregate(ready)));
                            }
                            return null;
                        });
            }
            current(job);
            require(
                    vectors.index(
                                    new VectorCommand(
                                            "RECONCILE",
                                            owner,
                                            document.publicId(),
                                            job.indexGeneration(),
                                            List.of(),
                                            null))
                            .success());
            finish(job, true, null);
        } catch (RuntimeException exception) {
            finish(job, false, "INDEX_FAILED");
        }
    }

    private static List<String> split(String text) {
        int[] points = text.codePoints().toArray();
        List<String> pieces = new java.util.ArrayList<>();
        for (int offset = 0; offset < points.length; offset += 448) {
            if (pieces.size() >= 256) {
                throw new DomainException(DomainErrorCode.INVALID);
            }
            pieces.add(new String(points, offset, Math.min(512, points.length - offset)));
        }
        return pieces;
    }

    private void current(IndexJobEntity expected) {
        var job = repositories.indexJob.findById(expected.id()).entity();
        var doc = repositories.knowledgeDocument.findById(job.documentId()).entity();
        if (!"RUNNING".equals(job.status())
                || !job.leaseFence().equals(expected.leaseFence())
                || job.leaseUntil().isBefore(now())
                || doc.deletedAt() != null
                || !doc.indexGeneration().equals(job.indexGeneration())) {
            throw new DomainException(DomainErrorCode.CONFLICT);
        }
    }

    private void state(IndexJobEntity job, String next, String error) {
        transactions.mutate(
                job.userId(),
                account -> {
                    current(job);
                    var doc = repositories.knowledgeDocument.findById(job.documentId()).entity();
                    Transactions.require(
                            writes.saveKnowledgeDocument(
                                    new KnowledgeDocumentAggregate(
                                            doc.transition(next, doc.extractedText(), error))));
                    events.append(
                            account,
                            "knowledge.changed",
                            doc.publicId(),
                            doc.version() + 1,
                            null,
                            "{}");
                    return null;
                });
    }

    private void finish(IndexJobEntity expected, boolean success, String error) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    var old = repositories.indexJob.findById(expected.id()).entity();
                    var doc = repositories.knowledgeDocument.findById(old.documentId()).entity();
                    if ("RUNNING".equals(old.status())
                            && old.leaseFence().equals(expected.leaseFence())) {
                        boolean same = doc.indexGeneration().equals(old.indexGeneration());
                        var done =
                                new IndexJobEntity(
                                        old.id(),
                                        old.publicId(),
                                        old.userId(),
                                        old.documentId(),
                                        old.indexGeneration(),
                                        old.jobType(),
                                        success ? "SUCCEEDED" : "FAILED",
                                        old.attemptCount(),
                                        old.maxAttempts(),
                                        old.nextAttemptAt(),
                                        old.leaseOwner(),
                                        old.leaseFence(),
                                        null,
                                        error,
                                        old.createdAt(),
                                        old.updatedAt(),
                                        old.version());
                        Transactions.require(writes.saveIndexJob(new IndexJobAggregate(done)));
                        if (same
                                && (("DELETE".equals(old.jobType()) && doc.deletedAt() != null)
                                        || doc.deletedAt() == null)) {
                            String next =
                                    "DELETE".equals(old.jobType())
                                            ? (success ? "DELETED" : "DELETING")
                                            : (success ? "READY" : "FAILED");
                            Transactions.require(
                                    writes.saveKnowledgeDocument(
                                            new KnowledgeDocumentAggregate(
                                                    doc.transition(
                                                            next,
                                                            "DELETED".equals(next)
                                                                    ? null
                                                                    : doc.extractedText(),
                                                            error))));
                        }
                    }
                    events.append(
                            account,
                            "knowledge.changed",
                            doc.publicId(),
                            doc.version() + 1,
                            null,
                            "{}");
                    return null;
                });
    }

    /**
     * 对账项目私有索引并恢复未知任务状态。
     *
     * @author AIGenerator
     */
    public void reconcile() {
        sweepOrphans();
        for (var row :
                repositories.indexJob.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "RUNNING")
                                .where("lease_until", "LT", now()))) {
            finish(row.entity(), false, "INDEX_INTERRUPTED");
        }
        var documents =
                repositories.knowledgeDocument.query(
                        QueryValue.all("id", 5).where("id", "GT", reconcileCursor.get()));
        if (documents.isEmpty()) {
            reconcileCursor.set(0);
        }
        for (var row : documents) {
            reconcileCursor.set(row.entity().id());
            var doc = row.entity();
            String owner = repositories.userAccount.findById(doc.userId()).entity().publicId();
            if (doc.deletedAt() != null) {
                var reconciled =
                        vectors.index(
                                new VectorCommand(
                                        "DELETE",
                                        owner,
                                        doc.publicId(),
                                        doc.indexGeneration(),
                                        List.of(),
                                        null));
                if (!reconciled.success()) {
                    return;
                }
                var erased = files.store(new FileCommand("DELETE", null, null, doc.storageKey()));
                if (!erased.success()) {
                    return;
                }
                transactions.mutate(
                        doc.userId(),
                        account -> {
                            var current =
                                    repositories.knowledgeDocument.findById(doc.id()).entity();
                            if (current.deletedAt() != null
                                    && (!"DELETED".equals(current.status())
                                            || current.extractedText() != null)) {
                                Transactions.require(
                                        writes.saveKnowledgeDocument(
                                                new KnowledgeDocumentAggregate(
                                                        current.transition(
                                                                "DELETED", null, null))));
                            }
                            for (var chunk :
                                    repositories.knowledgeChunk.query(
                                            QueryValue.all("id", 200)
                                                    .where("document_id", "EQ", doc.id())
                                                    .where("deleted_at", "NULL", null))) {
                                var old = chunk.entity();
                                var gone =
                                        new KnowledgeChunkEntity(
                                                old.id(),
                                                old.publicId(),
                                                old.userId(),
                                                old.documentId(),
                                                old.indexGeneration(),
                                                old.chunkNo(),
                                                "",
                                                old.contentSha256(),
                                                old.estimatedTokens(),
                                                old.vectorKey(),
                                                "DELETED",
                                                now(),
                                                old.createdAt(),
                                                old.updatedAt(),
                                                old.version());
                                Transactions.require(
                                        writes.saveKnowledgeChunk(
                                                new KnowledgeChunkAggregate(gone)));
                            }
                            events.append(
                                    account,
                                    "knowledge.cleaned",
                                    current.publicId(),
                                    current.version() + 1,
                                    null,
                                    "{}");
                            return null;
                        });
            } else {
                var reconciled =
                        vectors.index(
                                new VectorCommand(
                                        "RECONCILE",
                                        owner,
                                        doc.publicId(),
                                        doc.indexGeneration(),
                                        List.of(),
                                        null));
                if (!reconciled.success()) {
                    return;
                }
            }
        }
    }

    private void sweepOrphans() {
        var result = files.store(new FileCommand("SCAN", null, null, fileCursor.get()));
        if (!result.success()) {
            return;
        }
        var entries = Json.read(result.data().text());
        if (entries.isEmpty()) {
            fileCursor.set("");
        }
        for (var entry : entries) {
            String key = entry.path("key").asText();
            fileCursor.set(key);
            if (entry.path("modified").asLong() > System.currentTimeMillis() - 86400000L) {
                continue;
            }
            if (repositories
                    .knowledgeDocument
                    .query(QueryValue.all("id", 1).where("storage_key", "EQ", key))
                    .isEmpty()) {
                files.store(new FileCommand("DELETE", null, null, key));
            }
        }
    }

    private static void require(boolean success) {
        if (!success) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
