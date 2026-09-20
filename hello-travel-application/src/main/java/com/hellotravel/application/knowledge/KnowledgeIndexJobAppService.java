package com.hellotravel.application.knowledge;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.assembler.FileCommandAppAssembler;
import com.hellotravel.application.knowledge.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.assembler.KnowledgeEmbeddingAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeDomainParamAssembler;
import com.hellotravel.application.knowledge.assembler.VectorCommandAppAssembler;
import com.hellotravel.application.knowledge.command.VectorItemCommand;
import com.hellotravel.application.knowledge.adaptor.KnowledgeEmbeddingAgent;
import com.hellotravel.util.JsonUtil;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
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
public final class KnowledgeIndexJobAppService {

    public final KnowledgeDocumentRepository knowledgeDocument;
    public final KnowledgeChunkRepository knowledgeChunk;
    public final IndexJobRepository indexJob;
    private final UserAccountRepository userAccountRepository;
    private final KnowledgeDomainService knowledgeDomainService;
    private final KnowledgeDomainParamAssembler knowledgeDomainParamAssembler;
    private final Transactions transactions;
    private final SyncEventPublisher events;
    private final KnowledgeEmbeddingAgent embeddingAgent;
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

    private final FileCommandAppAssembler fileCommandAppAssembler;
    private final KnowledgeEmbeddingAppAssembler embeddingAssembler;
    private final VectorCommandAppAssembler vectorCommandAppAssembler;

    public KnowledgeIndexJobAppService(
            KnowledgeDomainService knowledgeDomainService,
            KnowledgeDomainParamAssembler knowledgeDomainParamAssembler,
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob,
            UserAccountRepository userAccountRepository,
            Transactions transactions,
            SyncEventPublisher events,
            KnowledgeEmbeddingAgent embeddingAgent,
            VectorOutAdaptor vectors,
            FileOutAdaptor files,
            FileCommandAppAssembler fileCommandAppAssembler,
            KnowledgeEmbeddingAppAssembler embeddingAssembler,
            VectorCommandAppAssembler vectorCommandAppAssembler) {
        this.knowledgeDomainService = knowledgeDomainService;
        this.knowledgeDomainParamAssembler = knowledgeDomainParamAssembler;
        this.knowledgeDocument = knowledgeDocument;
        this.knowledgeChunk = knowledgeChunk;
        this.indexJob = indexJob;
        this.userAccountRepository = userAccountRepository;
        this.transactions = transactions;
        this.events = events;
        this.embeddingAgent = embeddingAgent;
        this.vectors = vectors;
        this.files = files;
        this.fileCommandAppAssembler = fileCommandAppAssembler;
        this.embeddingAssembler = embeddingAssembler;
        this.vectorCommandAppAssembler = vectorCommandAppAssembler;
    }

    /**
     * 执行后台任务并保留租约与代次保护。
     *
     * @author AIGenerator
     * @param jobId 可信内部主键
     */
    public void execute(Long jobId) {
        // 1. 取得当前索引任务快照，供本段后续处理使用。
        IndexJobEntity job = claim(jobId);
        // 2. 未领取到当前任务则立即结束，避免无租约索引。
        if (job == null) {
            return;
        }
        // 3. 按可信内部标识读取知识文档当前快照。
        var document = knowledgeDocument.findById(job.documentId()).entity();
        // 4. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try {
            // 1. 按删除补偿场景进入对应职责分支。
            if ("DELETE".equals(job.jobType())) {
                deleteArtifacts(job, document);
                return;
            }
            // 2. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
            current(job);
            // 3. 执行state职责步骤，并把失败交给所属事务或入口处理。
            state(job, "INDEXING", null);
            // 4. 准备当前操作的正文或受限拼接容器。
            var text = document.extractedText();
            // 5. 核对输入或读取结果的存在性，失败中止当前处理。
            if (text == null || text.isBlank()) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 6. 按Unicode码点切块，防止拆断字符及无上限分块。
            List<String> pieces = split(text);
            String owner = userAccountRepository.findById(job.userId()).entity().publicId();
            // 7. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
            for (int offset = 0; offset < pieces.size(); offset += 16) {
                indexBatch(job, document, owner, pieces, offset);
            }
            // 8. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
            current(job);
            // 9. 执行require职责步骤，并把失败交给所属事务或入口处理。
            require(
                    vectors.index(
                                    vectorCommandAppAssembler.reconcile(
                                            owner, document, job.indexGeneration()))
                            .success());
            // 10. 以当前租约栅栏完成任务并记录稳定终态。
            finish(job, true, null);
        } catch (RuntimeException exception) {
            finish(job, false, "INDEX_FAILED");
        }
    }

    private static List<String> split(String text) {
        // 1. 读取正文的Unicode码点，切块不会拆断补充字符。
        int[] points = text.codePoints().toArray();
        List<String> pieces = new java.util.ArrayList<>();
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (int offset = 0; offset < points.length; offset += 448) {
            if (pieces.size() >= 256) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            pieces.add(new String(points, offset, Math.min(512, points.length - offset)));
        }
        // 3. 返回Unicode安全的有界分块清单，下一步骤按批次嵌入。
        return pieces;
    }

    private void current(IndexJobEntity expected) {
        // 1. 按可信内部标识读取索引任务当前快照。
        var job = indexJob.findById(expected.id()).entity();
        var doc = knowledgeDocument.findById(job.documentId()).entity();
        // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
        if (!"RUNNING".equals(job.status())
                || !job.leaseFence().equals(expected.leaseFence())
                || job.leaseUntil().isBefore(now())
                || doc.deletedAt() != null
                || !doc.indexGeneration().equals(job.indexGeneration())) {
            throw new ApplicationException(ApplicationErrorCode.CONFLICT);
        }
    }

    private void state(IndexJobEntity job, String next, String error) {
        transactions.mutate(
                job.userId(),
                account -> {
                    // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
                    current(job);
                    // 2. 按可信内部标识读取知识文档当前快照。
                    var doc =
                            knowledgeDocument
                                    .findById(job.documentId())
                                    .entity();
                    // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeDocument(
                                        knowledgeDomainParamAssembler.knowledgeDocument(
                                    new KnowledgeDocumentAggregate(doc)
                                            .transition(next, doc.extractedText(), error))))
                        .saved());
                    // 4. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "knowledge.changed",
                            doc.publicId(),
                            doc.version() + 1,
                            null,
                            "{}");
                    // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    private void finish(IndexJobEntity expected, boolean success, String error) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    // 1. 按可信内部标识读取索引任务当前快照。
                    var old = indexJob.findById(expected.id()).entity();
                    var doc = knowledgeDocument
                                    .findById(old.documentId())
                                    .entity();
                    // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
                    if ("RUNNING".equals(old.status())
                            && old.leaseFence().equals(expected.leaseFence())) {
                        boolean same = doc.indexGeneration().equals(old.indexGeneration());
                        var done = new IndexJobAggregate(old).complete(success, error).entity();
                        Transactions.require(
                                ApplicationFailures.required(
                                knowledgeDomainService.saveIndexJob(
                                        knowledgeDomainParamAssembler.indexJob(new IndexJobAggregate(done))))
                        .saved());
                        if (same
                                && (("DELETE".equals(old.jobType()) && doc.deletedAt() != null)
                                        || doc.deletedAt() == null)) {
                            String next =
                                    "DELETE".equals(old.jobType())
                                            ? (success ? "DELETED" : "DELETING")
                                            : (success ? "READY" : "FAILED");
                            Transactions.require(
                                    ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeDocument(
                                        knowledgeDomainParamAssembler.knowledgeDocument(
                                            new KnowledgeDocumentAggregate(doc)
                                                    .transition(
                                                            next,
                                                            "DELETED".equals(next)
                                                                    ? null
                                                                    : doc.extractedText(),
                                                            error))))
                        .saved());
                        }
                    }
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "knowledge.changed",
                            doc.publicId(),
                            doc.version() + 1,
                            null,
                            "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 对账项目私有索引并恢复未知任务状态。
     *
     * @author AIGenerator
     */
    public void reconcile() {
        // 1. 执行sweepOrphans职责步骤，并把失败交给所属事务或入口处理。
        sweepOrphans();
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                indexJob.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "RUNNING")
                                .where("lease_until", "LT", now()))) {
            finish(row.entity(), false, "INDEX_INTERRUPTED");
        }
        // 3. 读取知识文档，按当前用例条件限定查询窗口。
        var documents =
                knowledgeDocument.query(
                        QueryValue.all("id", 5).where("id", "GT", reconcileCursor.get()));
        // 4. 本查询窗口无文档时停止清理或修复扫描。
        if (documents.isEmpty()) {
            reconcileCursor.set(0);
        }
        // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row : documents) {
            reconcileCursor.set(row.entity().id());
            var doc = row.entity();
            String owner = userAccountRepository.findById(doc.userId()).entity().publicId();
            if (doc.deletedAt() != null) {
                var reconciled = vectors.index(vectorCommandAppAssembler.delete(owner, doc));
                if (!reconciled.success()) {
                    return;
                }
                var erased = files.store(fileCommandAppAssembler.delete(doc.storageKey()));
                if (!erased.success()) {
                    return;
                }
                transactions.mutate(
                        doc.userId(),
                        account -> {
                            // 1. 按可信内部标识读取知识文档当前快照。
                            var current =
                                    knowledgeDocument
                                            .findById(doc.id())
                                            .entity();
                            // 2. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
                            if (current.deletedAt() != null
                                    && (!"DELETED".equals(current.status())
                                            || current.extractedText() != null)) {
                                Transactions.require(
                                        ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeDocument(
                                        knowledgeDomainParamAssembler.knowledgeDocument(
                                                new KnowledgeDocumentAggregate(current)
                                                        .transition("DELETED", null, null))))
                        .saved());
                            }
                            // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                            for (var chunk :
                                    knowledgeChunk.query(
                                            QueryValue.all("id", 200)
                                                    .where("document_id", "EQ", doc.id())
                                                    .where("deleted_at", "NULL", null))) {
                                var old = chunk.entity();
                                var gone = new KnowledgeChunkAggregate(old).deleted(now()).entity();
                                Transactions.require(
                                        ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeChunk(
                                        knowledgeDomainParamAssembler.knowledgeChunk(
                                                new KnowledgeChunkAggregate(gone))))
                        .saved());
                            }
                            // 4. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                            events.append(
                                    account,
                                    "knowledge.cleaned",
                                    current.publicId(),
                                    current.version() + 1,
                                    null,
                                    "{}");
                            // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                            return null;
                        });
            } else {
                var reconciled =
                        vectors.index(
                                vectorCommandAppAssembler.reconcile(
                                        owner, doc, doc.indexGeneration()));
                if (!reconciled.success()) {
                    return;
                }
            }
        }
    }

    private void sweepOrphans() {
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var result = files.store(fileCommandAppAssembler.scan(fileCursor.get()));
        // 2. 依据下层标准结果的成功状态处理分支，避免继续使用无效数据。
        if (!result.success()) {
            return;
        }
        // 3. 取得本次外部数据返回的候选条目，供本段后续处理使用。
        var entries = JsonUtil.read(result.data().text());
        // 4. 没有私有文件时复位扫描游标，下次从首个文件重新检查。
        if (entries.isEmpty()) {
            fileCursor.set("");
        }
        // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var entry : entries) {
            String key = entry.path("key").asText();
            fileCursor.set(key);
            if (entry.path("modified").asLong() > System.currentTimeMillis() - 86400000L) {
                continue;
            }
            if (knowledgeDocument
                    .query(QueryValue.all("id", 1).where("storage_key", "EQ", key))
                    .isEmpty()) {
                files.store(fileCommandAppAssembler.delete(key));
            }
        }
    }

    private static void require(boolean success) {
        // 1. 索引下层失败必须中止当前任务，不以正常状态提交半成品。
        if (!success) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private IndexJobEntity claim(Long jobId) {
        // 1. 按可信内部标识读取索引任务当前快照。
        IndexJobEntity job =
                transactions.plain(
                        () -> {
                            // 1. 按可信内部标识读取索引任务当前快照。
                            var stored = indexJob.findById(jobId);
                            // 2. 只领取仍为PENDING的持久化任务，重复执行者不能同时占用租约。
                            if (stored == null || !"PENDING".equals(stored.entity().status())) {
                                return null;
                            }
                            // 3. 保留当前来源或状态快照，后续核对并发变更与重复执行。
                            var old = stored.entity();
                            var next = new IndexJobAggregate(old).claim(Ids.next(), now()).entity();
                            // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                            Transactions.require(
                                    ApplicationFailures.required(
                                knowledgeDomainService.saveIndexJob(
                                        knowledgeDomainParamAssembler.indexJob(new IndexJobAggregate(next))))
                        .saved());
                            // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                            return indexJob.findById(jobId).entity();
                        });
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return job;
    }

    private void deleteArtifacts(IndexJobEntity job, KnowledgeDocumentEntity document) {
        // 1. 按可信内部标识读取账号当前快照。
        String owner = userAccountRepository.findById(job.userId()).entity().publicId();
        // 2. 执行require职责步骤，并把失败交给所属事务或入口处理。
        require(vectors.index(vectorCommandAppAssembler.delete(owner, document)).success());
        // 3. 执行require职责步骤，并把失败交给所属事务或入口处理。
        require(files.store(fileCommandAppAssembler.delete(document.storageKey())).success());
        // 4. 以当前租约栅栏完成任务并记录稳定终态。
        finish(job, true, null);
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return;
    }

    private void indexBatch(
            IndexJobEntity job,
            KnowledgeDocumentEntity document,
            String owner,
            List<String> pieces,
            int offset) {
        // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
        current(job);
        // 2. 截取当前有界批次，外部调用保持在数据库事务之外。
        List<String> batch = pieces.subList(offset, Math.min(pieces.size(), offset + 16));
        var embedded = embeddingAgent.embed(embeddingAssembler.command(batch));
        // 3. 执行require职责步骤，并把失败交给所属事务或入口处理。
        require(embedded.success());
        // 4. 核对嵌入结果数量与输入批次一致，缺失结果不得继续写索引。
        if (embedded.data().vectors() == null || embedded.data().vectors().size() != batch.size()) {
            throw new ApplicationException(ApplicationErrorCode.FAILED);
        }
        // 5. 准备当前批次的向量条目，SQL正文与向量键保持一致。
        List<VectorItemCommand> items = new java.util.ArrayList<>();
        // 6. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (int i = 0; i < batch.size(); i++) {
            int number = offset + i;
            String body = batch.get(i);
            byte[] hash = Ids.hash(body);
            var chunk =
                    transactions.plain(
                            () -> {
                                // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
                                current(job);
                                // 2. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                                var entity =
                                        KnowledgeChunkAggregate.pending(
                                                        job,
                                                        number,
                                                        body,
                                                        hash,
                                                        ContextBudgetValue.estimate(body),
                                                        now())
                                                .entity();
                                // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                                Transactions.require(
                                        ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeChunk(
                                        knowledgeDomainParamAssembler.knowledgeChunk(
                                                new KnowledgeChunkAggregate(entity))))
                        .saved());
                                // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                                return entity;
                            });
            items.add(
                    vectorCommandAppAssembler.item(chunk, document, embedded.data().vectors().get(i)));
        }
        // 7. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
        current(job);
        // 8. 执行require职责步骤，并把失败交给所属事务或入口处理。
        require(
                vectors.index(vectorCommandAppAssembler.upsert(owner, document, job, items))
                        .success());
        // 9. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.plain(
                () -> {
                    // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
                    current(job);
                    // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var item : items) {
                        var old = knowledgeChunk
                                        .query(
                                                QueryValue.all("id", 1)
                                                        .where("vector_key", "EQ", item.key()))
                                        .get(0)
                                        .entity();
                        var ready = new KnowledgeChunkAggregate(old).ready().entity();
                        Transactions.require(
                                ApplicationFailures.required(
                                knowledgeDomainService.saveKnowledgeChunk(
                                        knowledgeDomainParamAssembler.knowledgeChunk(
                                        new KnowledgeChunkAggregate(ready))))
                        .saved());
                    }
                    // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }
}
