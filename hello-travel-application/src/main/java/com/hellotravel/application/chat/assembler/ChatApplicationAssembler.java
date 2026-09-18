package com.hellotravel.application.chat.assembler;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.chat.result.ConversationResult;
import com.hellotravel.application.chat.result.MessageResult;
import com.hellotravel.application.chat.result.RunResult;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;

import org.springframework.stereotype.Component;

/**
 * 对话内部快照转换为应用视图，不把实体或PO返回协议边界。
 *
 * @author AIGenerator
 */
@Component
public final class ChatApplicationAssembler {

    /**
     * 处理conversation对应的受控业务操作。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @return 当前操作的业务结果
     */
    public ConversationResult conversation(ConversationEntity entity) {
        return new ConversationResult(
                entity.publicId(),
                entity.title(),
                entity.version(),
                entity.historyEpoch(),
                entity.lastMessageSeq(),
                entity.lastActivityAt().toString() + "Z");
    }

    /**
     * 返回可公开的安全提示。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @return 当前操作的业务结果
     */
    public MessageResult message(MessageEntity entity) {
        return new MessageResult(
                entity.publicId(),
                entity.messageSeq(),
                entity.role(),
                entity.status(),
                entity.content(),
                entity.citationsJson(),
                entity.version());
    }

    /**
     * 处理run对应的受控业务操作。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @param conversationId 受控conversationId参数
     * @return 当前操作的业务结果
     */
    public RunResult run(ChatRunEntity entity, String conversationId) {
        return new RunResult(
                entity.publicId(),
                conversationId,
                entity.status(),
                entity.errorCode(),
                entity.attemptCount(),
                entity.contextSnapshotJson());
    }

    /**
     * 转换全量恢复起点及用户同步高水位。
     *
     * @param max 本次转换的max快照
     * @param sync 本次转换的sync快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult bootstrap(long max, long sync) {
        return new ChatResult(
                java.util.List.of(), java.util.List.of(), null, 0, false, max, 0, sync, null);
    }

    /**
     * 将持久对话页和请求上界整体转换为应用页。
     *
     * @param rows 本次转换的rows快照
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult tabs(java.util.List<ConversationAggregate> rows, ChatCommand command) {
        // 1. 从最后一个持久对话生成下一页游标，空页保留请求游标。
        long cursor = rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().id();
        // 2. 投影对话页并保留固定恢复上界。
        return new ChatResult(
                rows.stream().map(x -> conversation(x.entity())).toList(),
                java.util.List.of(),
                null,
                cursor,
                rows.size() == command.limit(),
                command.maxSeq() == null ? 0 : command.maxSeq(),
                0,
                0,
                null);
    }

    /**
     * 转换已提交对话变更及同事务同步高水位。
     *
     * @param entity 本次转换的entity快照
     * @param account 本次转换的account快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult changed(ConversationEntity entity, UserAccountEntity account) {
        return new ChatResult(
                java.util.List.of(conversation(entity)),
                java.util.List.of(),
                null,
                0,
                false,
                0,
                0,
                account.syncSeq(),
                null);
    }

    /**
     * 转换历史页，删除代次和固定上界来自已核验快照。
     *
     * @param entity 本次转换的entity快照
     * @param rows 本次转换的rows快照
     * @param command 本次转换的command快照
     * @param max 本次转换的max快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult history(
            ConversationEntity entity,
            java.util.List<MessageAggregate> rows,
            ChatCommand command,
            long max) {
        // 1. 从持久消息序号确定下一页游标。
        long cursor =
                rows.isEmpty() ? command.after() : rows.get(rows.size() - 1).entity().messageSeq();
        // 2. 同时返回对话删除代次和固定恢复上界，防止分页跨删除。
        return new ChatResult(
                java.util.List.of(conversation(entity)),
                rows.stream().map(x -> message(x.entity())).toList(),
                null,
                cursor,
                rows.size() == command.limit() && cursor < max,
                max,
                entity.historyEpoch(),
                0,
                null);
    }

    /**
     * 转换已受理生成任务及本轮消息快照。
     *
     * @param messages 本次转换的messages快照
     * @param run 本次转换的run快照
     * @param conversation 本次转换的conversation快照
     * @param account 本次转换的account快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult accepted(
            java.util.List<MessageEntity> messages,
            ChatRunEntity run,
            ConversationEntity conversation,
            UserAccountEntity account) {
        return new ChatResult(
                java.util.List.of(),
                messages.stream().map(this::message).toList(),
                run(run, conversation.publicId()),
                0,
                false,
                0,
                0,
                account.syncSeq(),
                null);
    }

    /**
     * 转换单次生成任务及未删除助手消息。
     *
     * @param run 本次转换的run快照
     * @param conversation 本次转换的conversation快照
     * @param output 本次转换的output快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult runSnapshot(
            ChatRunEntity run, ConversationEntity conversation, MessageEntity output) {
        return new ChatResult(
                java.util.List.of(),
                output.deletedAt() == null
                        ? java.util.List.of(message(output))
                        : java.util.List.of(),
                run(run, conversation.publicId()),
                0,
                false,
                0,
                0,
                0,
                run.contextSnapshotJson());
    }

    /**
     * 返回已完成且无需额外载荷的变更结果。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult completed() {
        return new ChatResult(
                java.util.List.of(), java.util.List.of(), null, 0, false, 0, 0, 0, null);
    }

    /**
     * 转换上下文预算及最近任务，缺少任务时只展示预算。
     *
     * @param conversation 本次转换的conversation快照
     * @param runs 本次转换的runs快照
     * @param budget 本次转换的budget快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResult context(
            ConversationEntity conversation, java.util.List<ChatRunAggregate> runs, String budget) {
        return new ChatResult(
                java.util.List.of(),
                java.util.List.of(),
                runs.isEmpty() ? null : run(runs.get(0).entity(), conversation.publicId()),
                0,
                false,
                0,
                0,
                0,
                budget);
    }

    /**
     * 将重放请求绑定到持久任务，忽略不适用的创建字段。
     *
     * @param command 本次转换的command快照
     * @param runId 本次转换的runId快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatCommand commandForRun(ChatCommand command, String runId) {
        return new ChatCommand(
                "RUN",
                command.userId(),
                command.sessionId(),
                null,
                runId,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                100);
    }
}
