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
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import org.springframework.stereotype.Component;

/**
 * HISTORY认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class ChatHistoryAppService extends ChatConversationSupport
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
  public ChatHistoryAppService(
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
    return "HISTORY";
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
    // 1. 取得当前对话或验证码快照，供本段后续处理使用。
    ConversationEntity c = owned(command.userId(), command.conversationId());
    // 2. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
    if (command.historyEpoch() != null && !c.historyEpoch().equals(command.historyEpoch())) {
      throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
    }
    // 3. 取得本次操作的容量上限，供本段后续处理使用。
    long max = command.maxSeq() == null ? c.lastMessageSeq() : command.maxSeq();
    var rows =
        messageRepository.query(
            QueryValue.all("message_seq", command.limit())
                .where("user_id", "EQ", command.userId())
                .where("conversation_id", "EQ", c.id())
                .where("deleted_at", "NULL", null)
                .where("message_seq", "GT", command.after())
                .where("message_seq", "LE", max));
    // 再读删除代次防止分页期间删除；普通流式更新不会触发重启。
    // 4. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
    if (!conversationRepository.findById(c.id()).entity().historyEpoch().equals(c.historyEpoch())) {
      throw new ApplicationException(ApplicationErrorCode.SYNC_RESET_REQUIRED);
    }
    // 5. 返回本段实际处理结果，保持本层输出契约。
    return assembler.history(c, rows, command, max);
  }
}
