package com.hellotravel.application.travel.execution;

import com.hellotravel.application.chat.support.ChatRepositories;
import com.hellotravel.application.chat.support.ChatWrites;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.model.travel.ModelDO;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * 生成租约/epoch栅栏、调用证据和回答落库，所有SQL事务独立于模型等待。
 *
 * @author AIGenerator
 */
@Component
public final class RunExecutionService {

    private final ChatRepositories chatRepositories;
    private final ChatWrites chatWrites;
    private final Transactions transactions;
    private final SyncEventPublisher events;

    public RunExecutionService(
            ChatWrites chatWrites,
            ChatRepositories chatRepositories,
            Transactions transactions,
            SyncEventPublisher events) {
        this.chatWrites = chatWrites;
        this.chatRepositories = chatRepositories;
        this.transactions = transactions;
        this.events = events;
    }

    /**
     * 领取待执行任务并提高租约栅栏。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public ChatRunEntity claim(String id) {
        // 1. 读取生成任务，按当前用例条件限定查询窗口。
        var rows =
                chatRepositories.chatRun.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", id));
        // 2. 候选生成任务不存在时拒绝领取，不创建虚假的执行实例。
        if (rows.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
        }
        // 3. 取得候选任务快照，领取时再次核验，供本段后续处理使用。
        ChatRunEntity selected = rows.get(0).entity();
        // 4. 进入短事务完成原子变更，外层统一负责提交、回滚与失败转换。
        return transactions.mutate(
                selected.userId(),
                account -> {
                    // 1. 按可信内部标识读取生成任务当前快照。
                    ChatRunEntity current =
                            chatRepositories.chatRun.findById(selected.id()).entity();
                    ConversationEntity c =
                            chatRepositories
                                    .conversation
                                    .findById(current.conversationId())
                                    .entity();
                    // 2. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
                    if (c.deletedAt() != null
                            || !c.memoryEpoch().equals(current.memoryEpochAtStart())) {
                        throw new ApplicationException(ApplicationErrorCode.CONFLICT);
                    }
                    // 3. 核对实体当前状态与允许的操作，失败中止当前处理。
                    if (!"ACCEPTED".equals(current.status())) {
                        throw new ApplicationException(ApplicationErrorCode.CONFLICT);
                    }
                    // 4. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    ChatRunEntity next = new ChatRunAggregate(current).claim(Ids.next()).entity();
                    // 5. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(chatWrites.saveChatRun(new ChatRunAggregate(next)));
                    // 6. 按可信内部标识读取消息当前快照。
                    MessageEntity output =
                            chatRepositories.message.findById(next.assistantMessageId()).entity();
                    // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveMessage(
                                    new MessageAggregate(output)
                                            .progress(output.content(), "STREAMING", null)));
                    // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "run.started",
                            next.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    // 9. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return chatRepositories.chatRun.findById(current.id()).entity();
                });
    }

    /**
     * 验证运行栅栏及对话记忆代次，阻止过期输出。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @return 当前操作的业务结果
     */
    public ChatRunEntity requireCurrent(ChatRunEntity expected) {
        // 1. 按可信内部标识读取生成任务当前快照。
        ChatRunEntity current = chatRepositories.chatRun.findById(expected.id()).entity();
        ConversationEntity c =
                chatRepositories.conversation.findById(expected.conversationId()).entity();
        // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
        if (current.leaseUntil() == null
                || current.leaseUntil().isBefore(now())
                || !"RUNNING".equals(current.status())
                || !current.leaseFence().equals(expected.leaseFence())
                || !current.attemptCount().equals(expected.attemptCount())
                || c.deletedAt() != null
                || !c.memoryEpoch().equals(expected.memoryEpochAtStart())) {
            throw new ApplicationException(ApplicationErrorCode.CONFLICT);
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return current;
    }

    /**
     * 保存当前步骤与有界上下文预算。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param node 当前工作流步骤
     * @param budget 有界上下文预算JSON
     */
    public void checkpoint(ChatRunEntity expected, String node, String budget) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    // 1. 读取当前持久化快照，避免依据外部旧快照直接写入。
                    ChatRunEntity current = requireCurrent(expected);
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveChatRun(
                                    new ChatRunAggregate(current)
                                            .checkpoint(
                                                    node,
                                                    Json.encode(
                                                            Map.of(
                                                                    "revision",
                                                                    "travel-v1",
                                                                    "node",
                                                                    node)),
                                                    budget)));
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "context.updated",
                            expected.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 更新助手正文与状态，拒绝向已删除消息写入。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param text 有界文本内容
     * @return 当前操作的业务结果
     */
    public boolean progress(ChatRunEntity expected, String text) {
        try {
            // 1. 进入受控事务处理，结果与回滚责任保持清晰。
            transactions.mutate(
                    expected.userId(),
                    account -> {
                        // 1. 读取当前持久化快照，避免依据外部旧快照直接写入。
                        ChatRunEntity current = requireCurrent(expected);
                        // 每次草稿落库同时续租；lease仅由相同fence持有者推进。
                        // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                        Transactions.require(
                                chatWrites.saveChatRun(
                                        new ChatRunAggregate(current)
                                                .checkpoint(
                                                        current.graphNode(),
                                                        current.stateJson(),
                                                        current.contextSnapshotJson())));
                        // 3. 按可信内部标识读取消息当前快照。
                        MessageEntity output =
                                chatRepositories
                                        .message
                                        .findById(expected.assistantMessageId())
                                        .entity();
                        // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                        Transactions.require(
                                chatWrites.saveMessage(
                                        new MessageAggregate(output)
                                                .progress(text, "STREAMING", null)));
                        // 5. 按可信内部标识读取对话当前快照。
                        ConversationEntity c =
                                chatRepositories
                                        .conversation
                                        .findById(expected.conversationId())
                                        .entity();
                        // 6. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                        events.append(
                                account,
                                "message.progress",
                                c.publicId(),
                                output.version() + 1,
                                null,
                                Json.encode(
                                        Map.of(
                                                "messageId",
                                                output.publicId(),
                                                "runId",
                                                expected.publicId())));
                        // 7. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                        return null;
                    });
            // 2. 返回本段实际处理结果，保持本层输出契约。
            return true;
        } catch (ApplicationException exception) {
            return false;
        }
    }

    /**
     * 在模型调用前保存尝试证据。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param stage 受控stage参数
     * @param call 本尝试内的压缩调用编号
     * @param estimated 受控estimated参数
     * @return 当前操作的业务结果
     */
    public String invocation(ChatRunEntity expected, String stage, int call, int estimated) {
        return transactions.plain(
                () -> {
                    // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
                    requireCurrent(expected);
                    // 2. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    ModelInvocationEntity created =
                            ModelInvocationAggregate.started(
                                            expected.userId(),
                                            expected.conversationId(),
                                            expected.id(),
                                            stage,
                                            expected.attemptCount(),
                                            call,
                                            estimated,
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                                    .entity();
                    // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveModelInvocation(new ModelInvocationAggregate(created)));
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return created.publicId();
                });
    }

    /**
     * 记录供应商实际返回的用量和调用结果。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @param response HTTP响应
     * @param latency 受控latency参数
     * @param success 受控success参数
     */
    public void invocationComplete(String id, ModelDO response, long latency, boolean success) {
        transactions.plain(
                () -> {
                    // 1. 读取模型调用证据，按当前用例条件限定查询窗口。
                    var current =
                            chatRepositories
                                    .modelInvocation
                                    .query(QueryValue.all("id", 1).where("public_id", "EQ", id))
                                    .get(0)
                                    .entity();
                    ModelInvocationEntity next =
                            current.completed(
                                    response == null ? current.modelName() : response.model(),
                                    response == null ? null : response.inputTokens(),
                                    response == null ? null : response.outputTokens(),
                                    (int) Math.min(Integer.MAX_VALUE, latency),
                                    response == null ? null : response.providerId(),
                                    success ? "SUCCEEDED" : "FAILED",
                                    success ? null : "MODEL_UNAVAILABLE",
                                    now());
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveModelInvocation(new ModelInvocationAggregate(next)));
                    // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 原子保存完整回答、任务终态和记忆提取任务。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param text 有界文本内容
     * @param citations 经校验的来源引用
     * @param response HTTP响应
     * @param budget 有界上下文预算JSON
     */
    public void finalizeAnswer(
            ChatRunEntity expected,
            String text,
            String citations,
            ModelDO response,
            String budget) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    // 1. 读取当前持久化快照，避免依据外部旧快照直接写入。
                    ChatRunEntity current = requireCurrent(expected);
                    ChatRunEntity finished =
                            new ChatRunAggregate(
                                            current.checkpoint(
                                                    "finalize", current.stateJson(), budget))
                                    .finish("COMPLETED", null)
                                    .entity();
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(chatWrites.saveChatRun(new ChatRunAggregate(finished)));
                    // 3. 按可信内部标识读取消息当前快照。
                    MessageEntity output =
                            chatRepositories
                                    .message
                                    .findById(expected.assistantMessageId())
                                    .entity();
                    // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveMessage(
                                    new MessageAggregate(output)
                                            .progress(text, "COMPLETED", citations)));
                    // 5. 按可信内部标识读取对话当前快照。
                    ConversationEntity c =
                            chatRepositories
                                    .conversation
                                    .findById(expected.conversationId())
                                    .entity();
                    // 6. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "message.completed",
                            c.publicId(),
                            output.version() + 1,
                            null,
                            Json.encode(Map.of("runId", expected.publicId())));
                    // 7. 同事务登记可恢复后台任务，外部调用在提交之后执行。
                    events.outbox(
                            account.id(),
                            "MEMORY_EXTRACT",
                            expected.publicId() + ":" + expected.attemptCount(),
                            Json.encode(Map.of("runId", expected.publicId())));
                    // 8. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 保留回答草稿并记录当前尝试失败。
     *
     * @author AIGenerator
     * @param expected 绑定当前尝试及租约栅栏的任务
     * @param error 待分类的失败
     */
    public void fail(ChatRunEntity expected, String error) {
        transactions.mutate(
                expected.userId(),
                account -> {
                    // 1. 按可信内部标识读取生成任务当前快照。
                    ChatRunEntity current =
                            chatRepositories.chatRun.findById(expected.id()).entity();
                    // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
                    if ("RUNNING".equals(current.status())
                            && current.leaseFence().equals(expected.leaseFence())) {
                        Transactions.require(
                                chatWrites.saveChatRun(
                                        new ChatRunAggregate(current).finish("FAILED", error)));
                        MessageEntity output =
                                chatRepositories
                                        .message
                                        .findById(current.assistantMessageId())
                                        .entity();
                        Transactions.require(
                                chatWrites.saveMessage(
                                        new MessageAggregate(output)
                                                .progress(
                                                        output.content(),
                                                        "FAILED",
                                                        output.citationsJson())));
                    }
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            account,
                            "run.failed",
                            current.publicId(),
                            current.version() + 1,
                            null,
                            "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 将租约过期任务标为中断，等待用户显式重试。
     *
     * @author AIGenerator
     */
    public void recoverExpired() {
        for (var row :
                chatRepositories.chatRun.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "RUNNING")
                                .where("lease_until", "LT", now()))) {
            transactions.mutate(
                    row.entity().userId(),
                    account -> {
                        // 1. 按可信内部标识读取生成任务当前快照。
                        ChatRunEntity current =
                                chatRepositories.chatRun.findById(row.entity().id()).entity();
                        // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
                        if ("RUNNING".equals(current.status())
                                && current.leaseUntil().isBefore(now())) {
                            Transactions.require(
                                    chatWrites.saveChatRun(
                                            new ChatRunAggregate(current)
                                                    .finish("INTERRUPTED", "LEASE_LOST")));
                            MessageEntity output =
                                    chatRepositories
                                            .message
                                            .findById(current.assistantMessageId())
                                            .entity();
                            Transactions.require(
                                    chatWrites.saveMessage(
                                            new MessageAggregate(output)
                                                    .progress(
                                                            output.content(),
                                                            "INTERRUPTED",
                                                            output.citationsJson())));
                        }
                        // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                        events.append(
                                account,
                                "run.interrupted",
                                current.publicId(),
                                current.version() + 1,
                                null,
                                "{}");
                        // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                        return null;
                    });
        }
    }

    /**
     * 将发件箱未知领取后的待执行任务标为中断，用户可显式重试。
     *
     * @author AIGenerator
     */
    public void recoverAccepted() {
        for (var row :
                chatRepositories.chatRun.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "ACCEPTED")
                                .where("updated_at", "LT", now().minusMinutes(15)))) {
            var selected = row.entity();
            transactions.mutate(
                    selected.userId(),
                    account -> {
                        // 1. 按可信内部标识读取生成任务当前快照。
                        var current = chatRepositories.chatRun.findById(selected.id()).entity();
                        // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                        if ("ACCEPTED".equals(current.status())
                                && current.updatedAt().isBefore(now().minusMinutes(15))) {
                            Transactions.require(
                                    chatWrites.saveChatRun(
                                            new ChatRunAggregate(current)
                                                    .finish("INTERRUPTED", "DISPATCH_UNKNOWN")));
                            var answer =
                                    chatRepositories
                                            .message
                                            .findById(current.assistantMessageId())
                                            .entity();
                            if (answer.deletedAt() == null) {
                                Transactions.require(
                                        chatWrites.saveMessage(
                                                new MessageAggregate(answer)
                                                        .progress(
                                                                answer.content(),
                                                                "INTERRUPTED",
                                                                answer.citationsJson())));
                            }
                        }
                        // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                        events.append(
                                account,
                                "run.interrupted",
                                current.publicId(),
                                current.version() + 1,
                                null,
                                "{}");
                        // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                        return null;
                    });
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
