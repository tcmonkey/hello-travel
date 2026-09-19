package com.hellotravel.application.chat.memory.context;

import com.hellotravel.application.chat.support.ChatRepositories;
import com.hellotravel.application.chat.support.ChatWrites;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.memory.agent.MemoryAgent;
import com.hellotravel.application.chat.memory.assembler.MemoryAgentAssembler;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.chat.memory.support.MemoryRepositories;
import com.hellotravel.application.chat.memory.support.MemoryWrites;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.chat.sync.support.SyncEventPublisher;
import com.hellotravel.application.chat.travel.execution.RunExecutionService;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.domain.chat.model.entity.ModelInvocationEntity;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemoryFactSourceAggregate;
import com.hellotravel.domain.memory.model.aggregate.MemorySummaryAggregate;
import com.hellotravel.domain.memory.model.entity.MemoryFactEntity;
import com.hellotravel.domain.memory.model.entity.MemoryFactSourceEntity;
import com.hellotravel.domain.memory.model.entity.MemorySummaryEntity;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户+会话隔离的近期、来源摘要与显式长期记忆；删除代次即时阻断旧记忆。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryContextService {

    private final MemoryRepositories memoryRepositories;
    private final ChatRepositories chatRepositories;
    private final MemoryWrites memoryWrites;
    private final ChatWrites chatWrites;
    private final Transactions transactions;
    private final MemoryAgent memoryAgent;
    private final SyncEventPublisher events;
    private final ChatContextPolicy contextPolicy;
    private final MemoryAgentAssembler memoryAgentAssembler;

    public MemoryContextService(
            MemoryWrites memoryWrites,
            ChatWrites chatWrites,
            MemoryRepositories memoryRepositories,
            ChatRepositories chatRepositories,
            Transactions transactions,
            MemoryAgent memoryAgent,
            SyncEventPublisher events,
            ChatContextPolicy contextPolicy,
            MemoryAgentAssembler memoryAgentAssembler) {
        this.memoryWrites = memoryWrites;
        this.chatWrites = chatWrites;
        this.memoryRepositories = memoryRepositories;
        this.chatRepositories = chatRepositories;
        this.transactions = transactions;
        this.memoryAgent = memoryAgent;
        this.events = events;
        this.contextPolicy = contextPolicy;
        this.memoryAgentAssembler = memoryAgentAssembler;
    }

    /**
     * 处理recentEntities对应的受控业务操作。
     *
     * @author AIGenerator
     * @param run 绑定当前尝试及租约栅栏的任务
     * @return 当前操作的业务结果
     */
    public List<MessageEntity> recentEntities(ChatRunEntity run) {
        // 1. 按可信内部标识读取消息当前快照。
        var input = chatRepositories.message.findById(run.userMessageId()).entity();
        var rows =
                chatRepositories.message.query(
                        QueryValue.all("message_seq", 24)
                                .desc()
                                .where("user_id", "EQ", run.userId())
                                .where("conversation_id", "EQ", run.conversationId())
                                .where("status", "EQ", "COMPLETED")
                                .where("deleted_at", "NULL", null)
                                .where("message_seq", "LT", input.messageSeq()));
        var pairs = completePairs(rows.stream().map(x -> x.entity()).toList());
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return List.copyOf(pairs.subList(Math.max(0, pairs.size() - 12), pairs.size()));
    }

    /**
     * 处理older对应的受控业务操作。
     *
     * @author AIGenerator
     * @param run 绑定当前尝试及租约栅栏的任务
     * @return 当前操作的业务结果
     */
    public List<MessageEntity> older(ChatRunEntity run) {
        // 1. 取得最近原始消息的有界窗口，供本段后续处理使用。
        var recent = recentEntities(run);
        // 2. 缺少有效原始消息时返回空记忆视图，不生成无来源摘要。
        if (recent.isEmpty()) {
            return List.of();
        }
        // 3. 读取短期摘要，限定当前用户及查询窗口。
        var summaries =
                memoryRepositories.memorySummary.query(
                        QueryValue.all("covered_through_seq", 1)
                                .desc()
                                .where("user_id", "EQ", run.userId())
                                .where("conversation_id", "EQ", run.conversationId())
                                .where("memory_epoch", "EQ", run.memoryEpochAtStart())
                                .where("status", "EQ", "ACTIVE"));
        long after = summaries.isEmpty() ? 0 : summaries.get(0).entity().coveredThroughSeq();
        var rows =
                chatRepositories.message.query(
                        QueryValue.all("message_seq", 24)
                                .where("user_id", "EQ", run.userId())
                                .where("conversation_id", "EQ", run.conversationId())
                                .where("status", "EQ", "COMPLETED")
                                .where("deleted_at", "NULL", null)
                                .where("message_seq", "GT", after)
                                .where("message_seq", "LT", recent.get(0).messageSeq()));
        // 4. 返回本段实际处理结果，保持本层输出契约。
        return completePairs(rows.stream().map(x -> x.entity()).toList());
    }

    private List<MessageEntity> completePairs(List<MessageEntity> messages) {
        // 1. 取得按稳定序号组织的消息快照，供本段后续处理使用。
        var bySeq = new java.util.TreeMap<Long, MessageEntity>();
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var entity : messages) {
            bySeq.put(entity.messageSeq(), entity);
        }
        // 3. 取得有效用户输入与助手回复配对，供本段后续处理使用。
        List<MessageEntity> pairs = new ArrayList<>();
        // 4. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var entity : bySeq.values()) {
            var answer = bySeq.get(entity.messageSeq() + 1);
            if ("USER".equals(entity.role())
                    && answer != null
                    && "ASSISTANT".equals(answer.role())) {
                pairs.add(entity);
                pairs.add(answer);
            }
        }
        // 5. 返回顺序稳定的完整用户问答对，孤立输入或回复不进入摘要。
        return List.copyOf(pairs);
    }

    /**
     * 读取当前记忆代次的有界摘要。
     *
     * @author AIGenerator
     * @param run 绑定当前尝试及租约栅栏的任务
     * @return 归属和状态校验后的业务快照
     */
    public String summary(ChatRunEntity run) {
        // 1. 读取短期摘要，限定当前用户及查询窗口。
        var rows =
                memoryRepositories.memorySummary.query(
                        QueryValue.all("covered_through_seq", 1)
                                .desc()
                                .where("user_id", "EQ", run.userId())
                                .where("conversation_id", "EQ", run.conversationId())
                                .where("memory_epoch", "EQ", run.memoryEpochAtStart())
                                .where("status", "EQ", "ACTIVE"));
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return rows.isEmpty() ? "" : rows.get(0).entity().structuredContent();
    }

    /**
     * 逐条核对原始用户消息来源后返回有效长期事实。
     *
     * @author AIGenerator
     * @param run 绑定当前尝试及租约栅栏的任务
     * @return 当前操作的业务结果
     */
    public String facts(ChatRunEntity run) {
        // 1. 准备当前操作的正文或受限拼接容器。
        StringBuilder text = new StringBuilder();
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                memoryRepositories.memoryFact.query(
                        QueryValue.all("id", 20)
                                .desc()
                                .where("user_id", "EQ", run.userId())
                                .where("conversation_id", "EQ", run.conversationId())
                                .where("memory_epoch", "EQ", run.memoryEpochAtStart())
                                .where("status", "EQ", "ACTIVE"))) {
            var fact = row.entity();
            if (fact.expiresAt() != null && !fact.expiresAt().isAfter(now())) {
                continue;
            }
            boolean sourceValid = false;
            for (var source :
                    memoryRepositories.memoryFactSource.query(
                            QueryValue.all("id", 8).where("fact_id", "EQ", fact.id()))) {
                var original = chatRepositories.message.findById(source.entity().messageId());
                if (original != null
                        && original.entity().deletedAt() == null
                        && "USER".equals(original.entity().role())
                        && original.entity().userId().equals(run.userId())
                        && original.entity().conversationId().equals(run.conversationId())
                        && original.entity().version().equals(source.entity().messageVersion())) {
                    sourceValid = true;
                    break;
                }
            }
            if (sourceValid && text.length() + fact.content().length() < 2000) {
                text.append(fact.content()).append("\n");
            }
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return text.toString();
    }

    /**
     * 压缩有界对话输入并保存带代次的摘要。
     *
     * @author AIGenerator
     * @param run 绑定当前尝试及租约栅栏的任务
     * @param messages 本对话的有界消息集合
     * @param prior 当前有效摘要
     * @param call 本尝试内的压缩调用编号
     * @param coordinator 运行栅栏与调用证据管理器
     * @return 当前操作的业务结果
     */
    public String compress(
            ChatRunEntity run,
            List<MessageEntity> messages,
            String prior,
            int call,
            RunExecutionService coordinator) {
        // 1. 取得本轮已持久化的原始用户消息，供本段后续处理使用。
        String input =
                prior
                        + "\n"
                        + messages.stream()
                                .map(x -> x.role() + ":" + x.content())
                                .collect(java.util.stream.Collectors.joining("\n"));
        // 2. 压缩输入超过上限时中止，避免摘要任务自身耗尽上下文。
        if (ContextBudgetValue.estimate(input) > contextPolicy.compressionInputLimit()) {
            throw new ApplicationException(ApplicationErrorCode.CONTEXT_LIMIT);
        }
        // 3. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        String id =
                coordinator.invocation(
                        run,
                        ChatModelStage.MEMORY_SUMMARY.name(),
                        call,
                        ContextBudgetValue.estimate(input));
        long start = System.nanoTime();
        var result = memoryAgent.summarize(memoryAgentAssembler.summary(input));
        // 4. 执行invocationComplete职责步骤，并把失败交给所属事务或入口处理。
        coordinator.invocationComplete(
                id,
                result.success() ? result.data() : null,
                (System.nanoTime() - start) / 1000000,
                result.success());
        // 5. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()
                || result.data().text() == null
                || ContextBudgetValue.estimate(result.data().text()) > 6000) {
            throw new ApplicationException(ApplicationErrorCode.CONTEXT_LIMIT);
        }
        // 6. 取得正文解析或摘要的受限结果，供本段后续处理使用。
        String compact = result.data().text();
        var inputEntity = chatRepositories.message.findById(run.userMessageId()).entity();
        // 7. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.plain(
                () -> {
                    // 1. 重新核对任务代次、租约与删除状态，阻止旧执行者写回。
                    coordinator.requireCurrent(run);
                    // 2. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    MemorySummaryEntity entity =
                            MemorySummaryAggregate.compressed(
                                            run.userId(),
                                            run.conversationId(),
                                            run.memoryEpochAtStart(),
                                            messages.get(0).messageSeq(),
                                            messages.get(messages.size() - 1).messageSeq(),
                                            Json.encode(
                                                    Map.of(
                                                            "summary",
                                                            compact,
                                                            "scope",
                                                            "bounded-sources",
                                                            "sourceSeqs",
                                                            messages.stream()
                                                                    .map(
                                                                            x ->
                                                                                    x.messageSeq()
                                                                                            .toString())
                                                                    .toList())),
                                            ContextBudgetValue.estimate(compact),
                                            result.data().model(),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                                    .entity();
                    var previous =
                            memoryRepositories.memorySummary.query(
                                    QueryValue.all("id", 1)
                                            .where("user_id", "EQ", run.userId())
                                            .where("conversation_id", "EQ", run.conversationId())
                                            .where("memory_epoch", "EQ", run.memoryEpochAtStart())
                                            .where(
                                                    "covered_through_seq",
                                                    "EQ",
                                                    entity.coveredThroughSeq()));
                    // 3. 首次压缩创建来源范围明确的摘要；已有摘要沿用原始归属。
                    if (previous.isEmpty()) {
                        Transactions.require(
                                memoryWrites.saveMemorySummary(new MemorySummaryAggregate(entity)));
                    }
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 8. 返回本段实际处理结果，保持本层输出契约。
        return compact;
    }

    /**
     * 仅对用户明确要求记住的消息提取来源化事实，记录调用证据避免任务重放再次计费。
     *
     * @param publicRunId 来源任务对外标识
     * @author AIGenerator
     */
    public void extract(String publicRunId) {
        // 1. 读取生成任务，按当前用例条件限定查询窗口。
        var rows =
                chatRepositories.chatRun.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", publicRunId));
        // 2. 生成任务不存在时结束记忆提取，不为不存在的任务创建记忆。
        if (rows.isEmpty()) {
            return;
        }
        // 3. 取得已领取且受租约保护的生成任务，供本段后续处理使用。
        ChatRunEntity run = rows.get(0).entity();
        ConversationEntity c =
                chatRepositories.conversation.findById(run.conversationId()).entity();
        var input = chatRepositories.message.findById(run.userMessageId()).entity();
        // 用户明确要求记住才进入长期事实，避免隐式画像或把模型推测持久化。
        // 4. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
        if (c.deletedAt() != null
                || input.deletedAt() != null
                || !c.memoryEpoch().equals(run.memoryEpochAtStart())
                || !input.content().matches("(?s).*(请记住|记住[:：]).*")) {
            return;
        }
        // 5. 读取模型调用证据，按当前用例条件限定查询窗口。
        var prior =
                chatRepositories.modelInvocation.query(
                        QueryValue.all("id", 1)
                                .where("run_id", "EQ", run.id())
                                .where("run_attempt_no", "EQ", run.attemptCount())
                                .where(
                                        "stage",
                                        "EQ",
                                        ChatModelStage.MEMORY_EXTRACTION.name()));
        // 6. 已有本轮提取调用记录时停止自动执行，避免重复调用模型计费。
        if (!prior.isEmpty()) {
            return;
        }
        // 7. 先保存本次调用证据，任务重放不得自动再次计费。
        String callId = reserveExtraction(run, input);
        var output = memoryAgent.extract(memoryAgentAssembler.extraction(input));
        // 8. 先记录外部调用成功或失败，重放不会再次调用模型计费。
        recordExtraction(callId, output);
        // 9. 依据下层标准结果的成功状态处理分支，避免继续使用无效数据。
        if (!output.success()) {
            return;
        }
        // 10. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var data =
                Json.read(output.data().text().replace("```json", "").replace("```", "").strip());
        // 11. 只接受有上限的JSON事实数组，错误模型输出不写入长期记忆。
        if (!data.isArray() || data.size() > 8) {
            return;
        }
        // 12. 短事务内再次核对来源和记忆代次，再保存显式事实与证据。
        persistFacts(run, c, input, data);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void persistFacts(
            ChatRunEntity run,
            ConversationEntity c,
            MessageEntity input,
            com.fasterxml.jackson.databind.JsonNode data) {
        transactions.mutate(
                run.userId(),
                account -> {
                    // 1. 按可信内部标识读取对话当前快照。
                    var current = chatRepositories.conversation.findById(c.id()).entity();
                    var source = chatRepositories.message.findById(input.id()).entity();
                    // 2. 核对历史或记忆代次，分页与派生记忆不能跨删除边界使用。
                    if (current.deletedAt() == null
                            && current.memoryEpoch().equals(run.memoryEpochAtStart())
                            && source.deletedAt() == null
                            && source.version().equals(input.version())) {
                        for (var item : data) {
                            var proposed =
                                    MemoryFactAggregate.propose(
                                            item.path("key").asText(),
                                            item.path("category").asText(),
                                            item.path("excerpt").asText(),
                                            input.content());
                            if (proposed.isEmpty()) {
                                continue;
                            }
                            var proposal = proposed.get();
                            String key = proposal.key();
                            String excerpt = proposal.excerpt();
                            var existing =
                                    memoryRepositories.memoryFact.query(
                                            QueryValue.all("id", 1)
                                                    .where("user_id", "EQ", run.userId())
                                                    .where(
                                                            "conversation_id",
                                                            "EQ",
                                                            run.conversationId())
                                                    .where(
                                                            "memory_epoch",
                                                            "EQ",
                                                            run.memoryEpochAtStart())
                                                    .where("fact_key", "EQ", key));
                            if (!existing.isEmpty()) {
                                var old = existing.get(0).entity();
                                for (var evidence :
                                        memoryRepositories.memoryFactSource.query(
                                                QueryValue.all("id", 1000)
                                                        .where("fact_id", "EQ", old.id()))) {
                                    Transactions.require(
                                            memoryWrites.removeMemoryFactSource(
                                                    evidence.entity().id()));
                                }
                                var revised =
                                        new MemoryFactAggregate(old)
                                                .reviseExplicit(proposal, now())
                                                .entity();
                                Transactions.require(
                                        memoryWrites.saveMemoryFact(
                                                new MemoryFactAggregate(revised)));
                                var evidence =
                                        MemoryFactSourceAggregate.evidence(
                                                        run.userId(),
                                                        run.conversationId(),
                                                        old.id(),
                                                        input.id(),
                                                        excerpt,
                                                        input.version(),
                                                        now())
                                                .entity();
                                Transactions.require(
                                        memoryWrites.saveMemoryFactSource(
                                                new MemoryFactSourceAggregate(evidence)));
                                continue;
                            }
                            MemoryFactEntity fact =
                                    MemoryFactAggregate.explicit(run, proposal, now()).entity();
                            Transactions.require(
                                    memoryWrites.saveMemoryFact(new MemoryFactAggregate(fact)));
                            var saved =
                                    memoryRepositories
                                            .memoryFact
                                            .query(
                                                    QueryValue.all("id", 1)
                                                            .where(
                                                                    "public_id",
                                                                    "EQ",
                                                                    fact.publicId()))
                                            .get(0)
                                            .entity();
                            MemoryFactSourceEntity evidence =
                                    MemoryFactSourceAggregate.evidence(
                                                    run.userId(),
                                                    run.conversationId(),
                                                    saved.id(),
                                                    input.id(),
                                                    excerpt,
                                                    input.version(),
                                                    java.time.LocalDateTime.now(
                                                            java.time.ZoneOffset.UTC))
                                            .entity();
                            Transactions.require(
                                    memoryWrites.saveMemoryFactSource(
                                            new MemoryFactSourceAggregate(evidence)));
                        }
                    }
                    // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(account, "memory.updated", c.publicId(), c.version(), null, "{}");
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    private void recordExtraction(String callId, Result<ChatModelDO> output) {
        // 调用即使失败也留下证据，任务重放不会再次计费。
        transactions.plain(
                () -> {
                    // 1. 读取模型调用证据，按当前用例条件限定查询窗口。
                    var invocation =
                            chatRepositories
                                    .modelInvocation
                                    .query(QueryValue.all("id", 1).where("public_id", "EQ", callId))
                                    .get(0)
                                    .entity();
                    ModelInvocationEntity finished =
                            new ModelInvocationAggregate(invocation)
                                    .extractionCompleted(
                                            output.success()
                                                    ? output.data().model()
                                                    : invocation.modelName(),
                                            output.success() ? output.data().inputTokens() : null,
                                            output.success() ? output.data().outputTokens() : null,
                                            output.success() ? "SUCCEEDED" : "FAILED",
                                            now())
                                    .entity();
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            chatWrites.saveModelInvocation(new ModelInvocationAggregate(finished)));
                    // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    private String reserveExtraction(ChatRunEntity run, MessageEntity input) {
        // 1. 先保存本次调用证据，任务重放不得自动再次计费。
        String callId =
                transactions.plain(
                        () -> {
                            // 1. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                            ModelInvocationEntity invocation =
                                    ModelInvocationAggregate.extractionStarted(
                                                    run.userId(),
                                                    run.conversationId(),
                                                    run.id(),
                                                    run.attemptCount(),
                                                    ContextBudgetValue.estimate(input.content()),
                                                    java.time.LocalDateTime.now(
                                                            java.time.ZoneOffset.UTC),
                                                    java.time.LocalDateTime.now(
                                                            java.time.ZoneOffset.UTC))
                                            .entity();
                            // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                            Transactions.require(
                                    chatWrites.saveModelInvocation(
                                            new ModelInvocationAggregate(invocation)));
                            // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                            return invocation.publicId();
                        });
        // 2. 返回本段实际处理结果，保持本层输出契约。
        return callId;
    }
}
