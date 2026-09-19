package com.hellotravel.application.knowledge.usecase;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.document.file.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.document.file.assembler.FileCommandAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeApplicationAssembler;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeResult;
import com.hellotravel.application.knowledge.support.KnowledgeRepositories;
import com.hellotravel.application.knowledge.support.KnowledgeWrites;
import com.hellotravel.application.chat.sync.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

/**
 * MySQL保存原文和状态，Milvus仅保存隔离向量；删除先阻断检索再异步清理。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeActionOperations {

    private final KnowledgeRepositories knowledgeRepositories;
    private final KnowledgeWrites knowledgeWrites;
    private final Transactions transactions;
    private final SyncEventPublisher events;
    private final FileOutAdaptor files;
    private final FileCommandAssembler fileCommandAssembler;
    private final KnowledgeApplicationAssembler knowledgeApplicationAssembler;

    public KnowledgeActionOperations(
            KnowledgeWrites knowledgeWrites,
            KnowledgeRepositories knowledgeRepositories,
            Transactions transactions,
            SyncEventPublisher events,
            FileOutAdaptor files,
            FileCommandAssembler fileCommandAssembler,
            KnowledgeApplicationAssembler knowledgeApplicationAssembler) {
        this.knowledgeWrites = knowledgeWrites;
        this.knowledgeRepositories = knowledgeRepositories;
        this.transactions = transactions;
        this.events = events;
        this.files = files;
        this.fileCommandAssembler = fileCommandAssembler;
        this.knowledgeApplicationAssembler = knowledgeApplicationAssembler;
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
        // 1. 读取知识文档，限定当前用户及查询窗口。
        var rows =
                knowledgeRepositories.knowledgeDocument.query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", userId)
                                .where("public_id", "EQ", id)
                                .where("deleted_at", "NULL", null));
        // 2. 知识列表为空时结束有界分页，不制造额外文档。
        if (rows.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
        }
        // 3. 返回当前用户可访问的对象，已删除或越权对象不会进入后续操作。
        return rows.get(0).entity();
    }

    KnowledgeResult read(KnowledgeCommand command) {
        // 1. 读取并转换当前账号拥有的知识文档。
        return knowledgeApplicationAssembler.single(owned(command.userId(), command.documentId()), true);
    }

    KnowledgeResult retry(KnowledgeCommand command) {
        return change(command, true);
    }

    KnowledgeResult delete(KnowledgeCommand command) {
        return change(command, false);
    }

    private KnowledgeResult change(KnowledgeCommand command, boolean retry) {
        // 1. 在短事务内处理重试或删除，归属和版本必须在事务内复核。
        return transactions.mutate(
                command.userId(),
                account -> {
                    // 1. 取得当前代次的知识文档快照，供本段后续处理使用。
                    var document = owned(command.userId(), command.documentId());
                    // 2. 核对快照版本与预期版本，失败中止当前处理。
                    if (command.expectedVersion() == null
                            || !command.expectedVersion().equals(document.version())) {
                        throw new ApplicationException(ApplicationErrorCode.CONFLICT);
                    }
                    // 3. 仅允许失败文档重新入库；删除路径直接进入删除代次。
                    if (retry) {
                        if (!"FAILED".equals(document.status())) {
                            throw new ApplicationException(ApplicationErrorCode.CONFLICT);
                        }
                        document = new KnowledgeDocumentAggregate(document).reindex().entity();
                        Transactions.require(
                                knowledgeWrites.saveKnowledgeDocument(
                                        new KnowledgeDocumentAggregate(document)));
                        job(document, "INGEST");
                    } else {
                        document = document.erase();
                        Transactions.require(
                                knowledgeWrites.saveKnowledgeDocument(
                                        new KnowledgeDocumentAggregate(document)));
                        job(document, "DELETE");
                    }
                    // 4. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "knowledge.changed",
                            document.publicId(),
                            document.version() + 1,
                            null,
                            "{}");
                    // 5. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
                    return knowledgeApplicationAssembler.single(document, false);
                });
    }

    private void job(KnowledgeDocumentEntity document, String type) {
        // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
        IndexJobEntity job =
                IndexJobAggregate.pending(
                                document.userId(),
                                document.id(),
                                document.indexGeneration(),
                                type,
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                        .entity();
        // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
        Transactions.require(knowledgeWrites.saveIndexJob(new IndexJobAggregate(job)));
    }

    KnowledgeResult list(KnowledgeCommand command) {
        // 1. 取得有界查询的结果数量，供本段后续处理使用。
        int limit = Math.min(100, Math.max(1, command.limit()));
        var rows =
                knowledgeRepositories.knowledgeDocument.query(
                        QueryValue.all("id", limit)
                                .where("user_id", "EQ", command.userId())
                                .where("deleted_at", "NULL", null)
                                .where("id", "GT", command.after()));
        // 2. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
        return knowledgeApplicationAssembler.page(rows, command, limit);
    }

    KnowledgeResult upload(KnowledgeCommand command) {
        // 1. 只接受有界HTTPS来源地址，拒绝凭据或缺失主机的链接。
        if (command.sourceUrl() != null && !command.sourceUrl().isBlank()) {
            java.net.URI uri = java.net.URI.create(command.sourceUrl());
            if (command.sourceUrl().length() > 2048
                    || !"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
        }
        // 2. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var result = files.store(fileCommandAssembler.save(command));
        // 3. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 4. 解析受限附件，格式与容量约束由解析步骤核对。
        var parsed = result.data();
        // 5. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try {
            return transactions.mutate(
                    command.userId(),
                    account -> {
                        // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                        var document =
                                knowledgeApplicationAssembler.received(command, parsed).entity();
                        // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                        Transactions.require(
                                knowledgeWrites.saveKnowledgeDocument(
                                        new KnowledgeDocumentAggregate(document)));
                        // 3. 更新本次处理的局部数据或上下文，后续步骤读取同一快照。
                        document = owned(command.userId(), document.publicId());
                        // 4. 执行job职责步骤，并把失败交给所属事务或入口处理。
                        job(document, "INGEST");
                        // 5. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                        events.append(
                                account,
                                "knowledge.created",
                                document.publicId(),
                                document.version(),
                                null,
                                "{}");
                        // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
                        return knowledgeApplicationAssembler.single(document, true);
                    });
        } catch (RuntimeException exception) {
            files.store(fileCommandAssembler.delete(parsed.storageKey()));
            throw exception;
        }
    }
}
