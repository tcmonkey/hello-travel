package com.hellotravel.application.knowledge;

import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.assembler.FileCommandAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeDomainParamAssembler;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import org.springframework.stereotype.Component;

/**
 * RETRY认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class RetryKnowledgeDocumentAppService extends KnowledgeDocumentSupport
    implements KnowledgeActionHandler {

  /**
   * 注入该业务动作所需的受控协作。
   *
   * @param knowledgeDomainService 注入的受控协作。
   * @param knowledgeDomainParamAssembler 注入的受控协作。
   * @param knowledgeDocument 注入的受控协作。
   * @param knowledgeChunk 注入的受控协作。
   * @param indexJob 注入的受控协作。
   * @param transactions 注入的受控协作。
   * @param events 注入的受控协作。
   * @param files 注入的受控协作。
   * @param fileCommandAppAssembler 注入的受控协作。
   * @param knowledgeAppAssembler 注入的受控协作。
   * @author AIGenerator
   */
  public RetryKnowledgeDocumentAppService(
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
    super(
        knowledgeDomainService,
        knowledgeDomainParamAssembler,
        knowledgeDocument,
        knowledgeChunk,
        indexJob,
        transactions,
        events,
        files,
        fileCommandAppAssembler,
        knowledgeAppAssembler);
  }

  /**
   * 返回本类唯一处理的业务动作。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  @Override
  public String action() {
    return "RETRY";
  }

  /**
   * 执行本类唯一的业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 当前动作的应用结果
   * @author AIGenerator
   */
  @Override
  public KnowledgeAppResult execute(KnowledgeCommand command) {
    // 1. 固定当前用例为重试，避免共用分支根据外部动作字段改变行为。
    boolean retry = true;
    // 2. 在短事务内处理重试，归属和版本必须在事务内复核。
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
                ApplicationFailures.required(
                        knowledgeDomainService.saveKnowledgeDocument(
                            knowledgeDomainParamAssembler.knowledgeDocument(
                                new KnowledgeDocumentAggregate(document))))
                    .saved());
            job(document, "INGEST");
          } else {
            document = document.erase();
            Transactions.require(
                ApplicationFailures.required(
                        knowledgeDomainService.saveKnowledgeDocument(
                            knowledgeDomainParamAssembler.knowledgeDocument(
                                new KnowledgeDocumentAggregate(document))))
                    .saved());
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
          return knowledgeAppAssembler.single(document, false);
        });
  }
}
