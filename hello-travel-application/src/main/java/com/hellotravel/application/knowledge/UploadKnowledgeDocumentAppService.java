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
 * UPLOAD认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class UploadKnowledgeDocumentAppService extends KnowledgeDocumentSupport
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
  public UploadKnowledgeDocumentAppService(
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
    return "UPLOAD";
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
    var result = files.store(fileCommandAppAssembler.save(command));
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
            var document = knowledgeAppAssembler.received(command, parsed).entity();
            // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
            Transactions.require(
                ApplicationFailures.required(
                        knowledgeDomainService.saveKnowledgeDocument(
                            knowledgeDomainParamAssembler.knowledgeDocument(
                                new KnowledgeDocumentAggregate(document))))
                    .saved());
            // 3. 更新本次处理的局部数据或上下文，后续步骤读取同一快照。
            document = owned(command.userId(), document.publicId());
            // 4. 执行job职责步骤，并把失败交给所属事务或入口处理。
            job(document, "INGEST");
            // 5. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
            events.append(
                account, "knowledge.created", document.publicId(), document.version(), null, "{}");
            // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
            return knowledgeAppAssembler.single(document, true);
          });
    } catch (RuntimeException exception) {
      files.store(fileCommandAppAssembler.delete(parsed.storageKey()));
      throw exception;
    }
  }
}
