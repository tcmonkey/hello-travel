package com.hellotravel.application.chat;

import com.hellotravel.application.chat.assembler.ChatAppAssembler;
import com.hellotravel.application.chat.assembler.ChatDomainParamAssembler;
import com.hellotravel.application.chat.assembler.MemoryContextAppAssembler;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.result.ChatAppResult;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * ChatConversationSupport提供跨认证动作复用的受控校验与访问能力，不承载动作编排。
 *
 * @author AIGenerator
 */
abstract class ChatConversationSupport {

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final ConversationRepository conversationRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final MessageRepository messageRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final ChatRunRepository chatRunRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final UserAccountRepository userAccountRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final ChatDomainService chatDomainService;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final ChatDomainParamAssembler chatDomainParamAssembler;

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
  protected final ChatContextPolicy contextPolicy;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final ChatAppAssembler assembler;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final MemoryContextAppAssembler memoryContextAppAssembler;

  protected ChatConversationSupport(
      ChatDomainService chatDomainService,
      ChatDomainParamAssembler chatDomainParamAssembler,
      ConversationRepository conversationRepository,
      MessageRepository messageRepository,
      ChatRunRepository chatRunRepository,
      UserAccountRepository userAccountRepository,
      Transactions transactions,
      SyncEventPublisher events,
      ChatAppAssembler assembler,
      ChatContextPolicy contextPolicy,
      MemoryContextAppAssembler memoryContextAppAssembler) {
    this.chatDomainService = chatDomainService;
    this.chatDomainParamAssembler = chatDomainParamAssembler;
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.chatRunRepository = chatRunRepository;
    this.userAccountRepository = userAccountRepository;
    this.transactions = transactions;
    this.events = events;
    this.assembler = assembler;
    this.contextPolicy = contextPolicy;
    this.memoryContextAppAssembler = memoryContextAppAssembler;
  }

  /**
   * 读取指定账号拥有的未删除对象。
   *
   * @author AIGenerator
   * @param userId 认证账号主键
   * @param publicId 受控publicId参数
   * @return 归属和状态校验后的业务快照
   */
  public ConversationEntity owned(Long userId, String publicId) {
    // 1. 读取对话，限定当前用户及查询窗口。
    var found =
        conversationRepository.query(
            QueryValue.all("id", 1)
                .where("user_id", "EQ", userId)
                .where("public_id", "EQ", publicId)
                .where("deleted_at", "NULL", null));
    // 2. 对象不存在时按当前用例的NOT_FOUND契约拒绝操作。
    if (found.isEmpty()) {
      throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
    }
    // 3. 返回当前用户可访问的对象，已删除或越权对象不会进入后续操作。
    return found.get(0).entity();
  }

  protected ChatRunEntity run(ChatCommand command) {
    // 1. 读取生成任务，限定当前用户及查询窗口。
    var found =
        chatRunRepository.query(
            QueryValue.all("id", 1)
                .where("user_id", "EQ", command.userId())
                .where("public_id", "EQ", command.runId()));
    // 2. 对象不存在时按当前用例的NOT_FOUND契约拒绝操作。
    if (found.isEmpty()) {
      throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
    }
    // 3. 按可信内部标识读取对话当前快照。
    ConversationEntity conversation =
        conversationRepository.findById(found.get(0).entity().conversationId()).entity();
    // 4. 核对删除状态与当前记忆代次，失败中止当前处理。
    if (conversation.deletedAt() != null) {
      throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
    }
    // 5. 返回本段实际处理结果，保持本层输出契约。
    return found.get(0).entity();
  }

  protected void version(ConversationEntity c, Long expected) {
    // 1. 核对客户端预期版本，防止旧页面覆盖较新对话状态。
    if (expected == null || !c.version().equals(expected)) {
      throw new ApplicationException(ApplicationErrorCode.CONFLICT);
    }
  }

  protected List<ChatRunAggregate> active(ConversationEntity c) {
    return chatRunRepository.query(
        QueryValue.all("id", 1)
            .where("conversation_id", "EQ", c.id())
            .where("status", "IN", List.of("ACCEPTED", "RUNNING")));
  }

  /**
   * 读取模型运行及其所属对话、助手消息的统一快照。
   *
   * @param command 已完成输入校验的对话命令
   * @return 可供多个动作复用的运行结果
   * @author AIGenerator
   */
  protected ChatAppResult runSnapshot(ChatCommand command) {
    // 1. 读取并校验当前用户可访问的模型运行。
    ChatRunEntity run = run(command);
    // 2. 读取运行关联的对话与助手消息。
    ConversationEntity conversation =
        conversationRepository.findById(run.conversationId()).entity();
    MessageEntity output = messageRepository.findById(run.assistantMessageId()).entity();
    // 3. 组装统一快照，避免动作服务间形成直接依赖。
    return assembler.runSnapshot(run, conversation, output);
  }

  protected static LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
