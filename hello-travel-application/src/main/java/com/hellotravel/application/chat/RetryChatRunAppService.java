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
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.service.ChatDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.util.JsonUtil;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RETRY认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class RetryChatRunAppService extends ChatConversationSupport
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
  public RetryChatRunAppService(
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
  public ChatAppResult execute(ChatCommand command) {
    return transactions.mutate(
        command.userId(),
        account -> {
          // 1. 取得下层返回的标准结果，供本段后续处理使用。
          ChatRunEntity r = run(command);
          ConversationEntity c = conversationRepository.findById(r.conversationId()).entity();
          // 2. 核对输入或读取结果的存在性，失败中止当前处理。
          if (!active(c).isEmpty()
              || !chatRunRepository
                  .query(
                      QueryValue.all("id", 1)
                          .where("user_id", "EQ", command.userId())
                          .where("status", "IN", List.of("ACCEPTED", "RUNNING")))
                  .isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.BUSY);
          }
          // 3. 按可信内部标识读取消息当前快照。
          MessageEntity input = messageRepository.findById(r.userMessageId()).entity();
          MessageEntity output = messageRepository.findById(r.assistantMessageId()).entity();
          // 4. 核对关联对象归属与角色，拒绝跨账号或跨会话关联。
          if (input.deletedAt() != null
              || output.deletedAt() != null
              || !"USER".equals(input.role())
              || !"ASSISTANT".equals(output.role())) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
          }
          // 5. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          ChatRunEntity next = new ChatRunAggregate(r).retry(c.memoryEpoch()).entity();
          // 6. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveChatRun(
                          chatDomainParamAssembler.chatRun(new ChatRunAggregate(next))))
                  .saved());
          // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveMessage(
                          chatDomainParamAssembler.message(
                              new MessageAggregate(output).progress("", "ACCEPTED", null))))
                  .saved());
          // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(account, "run.retried", r.publicId(), r.version() + 1, null, "{}");
          // 9. 同事务登记可恢复后台任务，外部调用在提交之后执行。
          events.outbox(
              account.id(),
              "GENERATE",
              r.publicId() + ":" + next.attemptCount(),
              JsonUtil.encode(Map.of("runId", r.publicId())));
          // 10. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return runSnapshot(command);
        });
  }
}
