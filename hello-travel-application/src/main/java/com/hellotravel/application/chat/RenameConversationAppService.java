package com.hellotravel.application.chat;

import com.hellotravel.application.chat.assembler.ChatAppAssembler;
import com.hellotravel.application.chat.assembler.ChatDomainParamAssembler;
import com.hellotravel.application.chat.assembler.MemoryContextAppAssembler;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.result.ChatAppResult;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import org.springframework.stereotype.Component;

/**
 * RENAME认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class RenameConversationAppService extends ChatConversationSupport
    implements ChatActionHandler {

  /**
   * 注入该业务动作所需的受控协作。
   *
   * @param chatDomainService 注入的受控协作。
   * @param chatDomainParamAssembler 注入的受控协作。
   * @param conversationRepository 注入的受控协作。
   * @param messageRepository 注入的受控协作。
   * @param chatRunRepository 注入的受控协作。
   * @param userAccountRepository 注入的受控协作。
   * @param transactions 注入的受控协作。
   * @param events 注入的受控协作。
   * @param assembler 注入的受控协作。
   * @param contextPolicy 注入的受控协作。
   * @param memoryContextAppAssembler 注入的受控协作。
   * @author AIGenerator
   */
  public RenameConversationAppService(
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
    super(
        chatDomainService,
        chatDomainParamAssembler,
        conversationRepository,
        messageRepository,
        chatRunRepository,
        userAccountRepository,
        transactions,
        events,
        assembler,
        contextPolicy,
        memoryContextAppAssembler);
  }

  /**
   * 返回本类唯一处理的业务动作。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  @Override
  public String action() {
    return "RENAME";
  }

  /**
   * 执行本类唯一的业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 当前动作的应用结果
   * @author AIGenerator
   */
  @Override
  public ChatAppResult execute(ChatCommand command) {
    return transactions.mutate(
        command.userId(),
        account -> {
          // 1. 取得事务内重新加载的持久化快照，供本段后续处理使用。
          ConversationEntity stored = owned(account.id(), command.conversationId());
          // 2. 执行version职责步骤，并把失败交给所属事务或入口处理。
          version(stored, command.expectedVersion());
          // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveConversation(
                          chatDomainParamAssembler.conversation(
                              new ConversationAggregate(stored).rename(command.title()))))
                  .saved());
          // 4. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              account, "conversation.renamed", stored.publicId(), stored.version() + 1, null, "{}");
          // 5. 读取当前持久化快照，避免依据外部旧快照直接写入。
          ConversationEntity current = owned(account.id(), stored.publicId());
          // 6. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return assembler.changed(current, account);
        });
  }
}
