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
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
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
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * SUBMIT认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class SubmitMessageAppService extends ChatConversationSupport
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
  public SubmitMessageAppService(
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
    return "SUBMIT";
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
    // 1. 核对输入或读取结果的存在性，失败中止当前处理。
    if (command.text() == null || command.text().isBlank() || command.text().length() > 8000) {
      throw new ApplicationException(ApplicationErrorCode.INVALID);
    }
    // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
    try {
      UUID.fromString(command.requestKey());
    } catch (RuntimeException e) {
      throw new ApplicationException(ApplicationErrorCode.INVALID);
    }
    // 3. 计算请求正文摘要，绑定幂等键与实际内容。
    byte[] digest = Ids.hash(command.text());
    // 4. 进入短事务完成原子变更，外层统一负责提交、回滚与失败转换。
    return transactions.mutate(
        command.userId(),
        account -> {
          // 1. 取得当前对话或验证码快照，供本段后续处理使用。
          ConversationEntity c = owned(account.id(), command.conversationId());
          var replay =
              chatRunRepository.query(
                  QueryValue.all("id", 1)
                      .where("user_id", "EQ", account.id())
                      .where("conversation_id", "EQ", c.id())
                      .where("request_key", "EQ", command.requestKey()));
          // 2. 核对输入或读取结果的存在性，失败中止当前处理。
          if (!replay.isEmpty()) {
            if (!java.security.MessageDigest.isEqual(
                digest, replay.get(0).entity().requestDigest())) {
              throw new ApplicationException(ApplicationErrorCode.IDEMPOTENCY_CONFLICT);
            }
            // 即使幂等重放也提交一个无正文事件，保持账号序列没有补齐缺口。
            events.append(
                account,
                "run.replayed",
                replay.get(0).entity().publicId(),
                replay.get(0).entity().version(),
                null,
                "{}");
            return runSnapshot(assembler.commandForRun(command, replay.get(0).entity().publicId()));
          }
          // 3. 核对实体当前状态与允许的操作，失败中止当前处理。
          if (chatRunRepository
                  .query(
                      QueryValue.all("id", 8).where("status", "IN", List.of("ACCEPTED", "RUNNING")))
                  .size()
              >= 8) {
            throw new ApplicationException(ApplicationErrorCode.RATE_LIMITED);
          }
          // 4. 核对输入或读取结果的存在性，失败中止当前处理。
          if (!active(c).isEmpty()
              || !chatRunRepository
                  .query(
                      QueryValue.all("id", 1)
                          .where("user_id", "EQ", command.userId())
                          .where("status", "IN", List.of("ACCEPTED", "RUNNING")))
                  .isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.BUSY);
          }
          // 5. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          MessageEntity input = MessageAggregate.userInput(c, command.text(), now()).entity();
          MessageEntity output = MessageAggregate.assistantPlaceholder(c, now()).entity();
          // 6. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveConversation(
                          chatDomainParamAssembler.conversation(
                              new ConversationAggregate(c).appendPair())))
                  .saved());
          // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveMessage(
                          chatDomainParamAssembler.message(new MessageAggregate(input))))
                  .saved());
          // 8. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveMessage(
                          chatDomainParamAssembler.message(new MessageAggregate(output))))
                  .saved());
          // 9. 读取消息，按当前用例条件限定查询窗口。
          MessageEntity storedInput =
              messageRepository
                  .query(QueryValue.all("id", 1).where("public_id", "EQ", input.publicId()))
                  .get(0)
                  .entity();
          MessageEntity storedOutput =
              messageRepository
                  .query(QueryValue.all("id", 1).where("public_id", "EQ", output.publicId()))
                  .get(0)
                  .entity();
          ChatRunEntity run =
              ChatRunAggregate.accepted(
                      c,
                      command.sessionId(),
                      command.requestKey(),
                      digest,
                      storedInput,
                      storedOutput,
                      now())
                  .entity();
          // 10. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      chatDomainService.saveChatRun(
                          chatDomainParamAssembler.chatRun(new ChatRunAggregate(run))))
                  .saved());
          // 11. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              account,
              "messages.accepted",
              c.publicId(),
              c.version() + 1,
              null,
              JsonUtil.encode(Map.of("runId", run.publicId())));
          // 12. 同事务登记可恢复后台任务，外部调用在提交之后执行。
          events.outbox(
              account.id(),
              "GENERATE",
              run.publicId(),
              JsonUtil.encode(Map.of("runId", run.publicId())));
          // 13. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return assembler.accepted(List.of(input, output), run, c, account);
        });
  }
}
