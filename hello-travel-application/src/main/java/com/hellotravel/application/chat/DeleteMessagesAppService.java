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
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.util.JsonUtil;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * DELETE_MESSAGES认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class DeleteMessagesAppService extends ChatConversationSupport
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
  public DeleteMessagesAppService(
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
    return "DELETE_MESSAGES";
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
    boolean entire = false;
    // 1. 进入受控事务处理，结果与回滚责任保持清晰。
    transactions.mutate(
        command.userId(),
        account -> {
          // 1. 取得当前对话或验证码快照，供本段后续处理使用。
          ConversationEntity c = owned(account.id(), command.conversationId());
          // 2. 执行version职责步骤，并把失败交给所属事务或入口处理。
          version(c, command.expectedVersion());
          // 3. 取得当前租约下的运行快照，供本段后续处理使用。
          var running = active(c);
          // 4. 核对输入或读取结果的存在性，失败中止当前处理。
          if (!entire && !running.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.BUSY);
          }
          // 5. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
          for (var r : running) {
            Transactions.require(
                ApplicationFailures.required(
                        chatDomainService.saveChatRun(
                            chatDomainParamAssembler.chatRun(
                                new ChatRunAggregate(r.entity())
                                    .finish("CANCELLED", "CONVERSATION_DELETED"))))
                    .saved());
          }
          // 6. 局部删消息时校验稳定消息标识，全量清理走独立代次处理。
          if (!entire) {
            if (command.messageIds() == null
                || command.messageIds().isEmpty()
                || command.messageIds().size() > 100) {
              throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            var rows =
                messageRepository.query(
                    QueryValue.all("id", 100)
                        .where("user_id", "EQ", account.id())
                        .where("conversation_id", "EQ", c.id())
                        .where("public_id", "IN", command.messageIds())
                        .where("deleted_at", "NULL", null));
            if (rows.size() != new java.util.HashSet<>(command.messageIds()).size()) {
              throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
            }
            for (var m : rows) {
              Transactions.require(
                  ApplicationFailures.required(
                          chatDomainService.saveMessage(
                              chatDomainParamAssembler.message(
                                  new MessageAggregate(m.entity()).erase())))
                      .saved());
            }
          }
          // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveConversation(
                          chatDomainParamAssembler.conversation(
                              new ConversationAggregate(c).eraseHistory(entire))))
                  .saved());
          // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              account,
              entire ? "conversation.deleted" : "messages.deleted",
              c.publicId(),
              c.version() + 1,
              null,
              "{}");
          // 9. 同事务登记可恢复后台任务，外部调用在提交之后执行。
          events.outbox(
              account.id(),
              "MEMORY_REBUILD",
              c.publicId() + ":" + (c.memoryEpoch() + 1),
              JsonUtil.encode(Map.of("conversationId", c.id())));
          // 10. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return null;
        });
    // 2. 返回本段实际处理结果，保持本层输出契约。
    return assembler.completed();
  }
}
