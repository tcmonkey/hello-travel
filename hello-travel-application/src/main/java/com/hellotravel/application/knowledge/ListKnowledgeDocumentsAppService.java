package com.hellotravel.application.knowledge;

import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.knowledge.adaptor.FileOutAdaptor;
import com.hellotravel.application.knowledge.assembler.FileCommandAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeDomainParamAssembler;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import org.springframework.stereotype.Component;

/**
 * LIST认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class ListKnowledgeDocumentsAppService extends KnowledgeDocumentSupport
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
  public ListKnowledgeDocumentsAppService(
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
    return "LIST";
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
    // 1. 取得有界查询的结果数量，供本段后续处理使用。
    int limit = Math.min(100, Math.max(1, command.limit()));
    var rows =
        knowledgeDocument.query(
            QueryValue.all("id", limit)
                .where("user_id", "EQ", command.userId())
                .where("deleted_at", "NULL", null)
                .where("id", "GT", command.after()));
    // 2. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
    return knowledgeAppAssembler.page(rows, command, limit);
  }
}
