package com.hellotravel.application.knowledge;

import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.assembler.KnowledgeAppAssembler;
import com.hellotravel.application.knowledge.assembler.KnowledgeDomainParamAssembler;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.util.file.FileStorageException;
import com.hellotravel.util.file.FileStorageUtil;
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
   * 仅由上传动作使用的内部本地文件工具。
   *
   * @author AIGenerator
   */
  private final FileStorageUtil files;

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
   * @param files 本地文件通用工具。
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
      FileStorageUtil files,
      KnowledgeAppAssembler knowledgeAppAssembler) {
    super(
        knowledgeDomainService,
        knowledgeDomainParamAssembler,
        knowledgeDocument,
        knowledgeChunk,
        indexJob,
        transactions,
        events,
        knowledgeAppAssembler);
    this.files = files;
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
    // 2. 调用内部文件工具完成受限解析与原子保存，业务层保留文件结果的使用决定。
    var parsed = store(command);
    // 3. 在异常边界内持久化知识库聚合，失败时补偿本次落盘文件。
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
      deleteQuietly(parsed.storageKey());
      throw exception;
    }
  }

  private FileStorageUtil.StoredFile store(KnowledgeCommand command) {
    try {
      // 1. 使用通用文件工具处理字节与格式，知识库规则不进入工具实现。
      return files.store(command.filename(), command.bytes());
    } catch (FileStorageException exception) {
      // 2. 将技术工具失败转换为当前应用动作可公开的稳定错误。
      throw new ApplicationException(
          exception.reason() == FileStorageException.Reason.INVALID
              ? ApplicationErrorCode.INVALID
              : exception.reason() == FileStorageException.Reason.BUSY
                  ? ApplicationErrorCode.RATE_LIMITED
                  : ApplicationErrorCode.UNAVAILABLE);
    }
  }

  private void deleteQuietly(String storageKey) {
    try {
      // 1. 补偿只清理本次已落盘文件，原始业务异常仍由调用方保留。
      files.delete(storageKey);
    } catch (FileStorageException ignored) {
      // 2. 记录由外层统一异常链路完成，补偿失败不能覆盖原始事务失败。
    }
  }

}
