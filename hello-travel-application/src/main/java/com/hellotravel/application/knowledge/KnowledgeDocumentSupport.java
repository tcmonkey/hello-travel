package com.hellotravel.application.knowledge;

import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.assembler.FileCommandAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeDomainParamAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;

/**
 * KnowledgeDocumentSupport提供跨认证动作复用的受控校验与访问能力，不承载动作编排。
 *
 * @author AIGenerator
 */
abstract class KnowledgeDocumentSupport {

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  public final KnowledgeDocumentRepository knowledgeDocument;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  public final KnowledgeChunkRepository knowledgeChunk;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  public final IndexJobRepository indexJob;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final KnowledgeDomainService knowledgeDomainService;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final KnowledgeDomainParamAssembler knowledgeDomainParamAssembler;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final Transactions transactions;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final SyncEventPublisher events;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final FileOutAdaptor files;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final FileCommandAppAssembler fileCommandAppAssembler;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final KnowledgeAppAssembler knowledgeAppAssembler;

  protected KnowledgeDocumentSupport(
      KnowledgeDomainService knowledgeDomainService,
      KnowledgeDomainParamAssembler knowledgeDomainParamAssembler,
      KnowledgeDocumentRepository knowledgeDocument,
      KnowledgeChunkRepository knowledgeChunk,
      IndexJobRepository indexJob,
      Transactions transactions,
      SyncEventPublisher events,
      FileOutAdaptor files,
      FileCommandAppAssembler fileCommandAppAssembler,
      KnowledgeAppAssembler knowledgeAppAssembler) {
    this.knowledgeDomainService = knowledgeDomainService;
    this.knowledgeDomainParamAssembler = knowledgeDomainParamAssembler;
    this.knowledgeDocument = knowledgeDocument;
    this.knowledgeChunk = knowledgeChunk;
    this.indexJob = indexJob;
    this.transactions = transactions;
    this.events = events;
    this.files = files;
    this.fileCommandAppAssembler = fileCommandAppAssembler;
    this.knowledgeAppAssembler = knowledgeAppAssembler;
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
        knowledgeDocument.query(
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

  protected void job(KnowledgeDocumentEntity document, String type) {
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
    Transactions.require(
        ApplicationFailures.required(
                knowledgeDomainService.saveIndexJob(
                    knowledgeDomainParamAssembler.indexJob(new IndexJobAggregate(job))))
            .saved());
  }
}
