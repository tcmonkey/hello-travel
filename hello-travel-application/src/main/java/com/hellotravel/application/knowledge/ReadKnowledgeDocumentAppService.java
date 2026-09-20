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
import org.springframework.stereotype.Component;

/**
 * READ认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class ReadKnowledgeDocumentAppService extends KnowledgeDocumentSupport
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
  public ReadKnowledgeDocumentAppService(
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
    return "READ";
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
    // 1. 读取并转换当前账号拥有的知识文档。
    return knowledgeAppAssembler.single(owned(command.userId(), command.documentId()), true);
  }
}
