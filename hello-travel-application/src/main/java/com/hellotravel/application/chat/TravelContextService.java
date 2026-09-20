package com.hellotravel.application.chat;

import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.assembler.MemoryContextAppAssembler;
import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

import com.hellotravel.util.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 统一加载、压缩和核验旅行对话上下文，使三种回答路径共享同一预算规则。
 *
 * @author AIGenerator
 */
@Component
public final class TravelContextService {

    private final MessageRepository messageRepository;
    private final RunExecutionService runExecutionService;
    private final MemoryContextAppService memoryContextAppService;
    private final ChatContextPolicy contextPolicy;
    private final MemoryContextAppAssembler memoryContextAppAssembler;

    public TravelContextService(
            MessageRepository messageRepository,
            RunExecutionService runExecutionService,
            MemoryContextAppService memoryContextAppService,
            ChatContextPolicy contextPolicy,
            MemoryContextAppAssembler memoryContextAppAssembler) {
        this.messageRepository = messageRepository;
        this.runExecutionService = runExecutionService;
        this.memoryContextAppService = memoryContextAppService;
        this.contextPolicy = contextPolicy;
        this.memoryContextAppAssembler = memoryContextAppAssembler;
    }

    /**
     * 加载当前用户和会话隔离的输入、短期窗口、摘要及长期记忆。
     *
     * @param run 已领取的生成任务
     * @return 已准备的旅行上下文
     * @author AIGenerator
     */
    public Result<TravelConversationContext> prepare(ChatRunEntity run) {
        try {
            // 1. 重新核对租约和记忆代次，避免旧执行者继续构造上下文。
            runExecutionService.requireCurrent(run);
            TravelConversationContext context = new TravelConversationContext(run);
            // 2. 只读取当前用户与当前对话的消息和记忆快照。
            context.load(
                    messageRepository.findById(run.userMessageId()).entity().content(),
                    memoryContextAppService.recentEntities(run),
                    memoryContextAppService.summary(run),
                    memoryContextAppService.facts(run));
            // 3. 对短期窗口之外且尚未摘要化的完整问答对执行滚动压缩。
            var older = memoryContextAppService.older(run);
            if (!older.isEmpty()) {
                checkpoint(context, "rolling-summary");
                String summary =
                        memoryContextAppService.compress(
                                run, older, context.summary(), 1, runExecutionService);
                context.replaceHistory(summary, context.recent(), "COMPLETED");
            }
            // 4. 核验首次模型调用所需的上下文预算。
            ApplicationFailures.required(ensureBudget(context, "context_ready"));
            return Result.success(context);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.FAILED);
        }
    }

    /**
     * 在外部模型调用前计算预算，并在必要时压缩当前短期窗口。
     *
     * @param context 当前旅行上下文
     * @param checkpoint 当前业务阶段
     * @return 是否满足模型上下文预算
     * @author AIGenerator
     */
    public Result<Boolean> ensureBudget(TravelConversationContext context, String checkpoint) {
        try {
            // 1. 依据服务端规则、记忆、事实、资料和原始消息计算保守输入量。
            int estimated = estimate(context);
            context.estimatedTokens(estimated);
            var budget = contextPolicy.budget(estimated);
            // 2. 达到压缩阈值时仅压缩完整短期问答对，并保留原始消息事实源。
            if (budget.needsCompression() && !context.recent().isEmpty()) {
                runExecutionService.checkpoint(context.run(), "compressing", budgetJson(context));
                String summary = memoryContextAppService.compress(
                        context.run(), context.recent(), context.summary(), 2, runExecutionService);
                context.replaceHistory(summary, List.of(), "COMPLETED");
                context.estimatedTokens(estimate(context));
            }
            // 3. 压缩后仍无法容纳安全和回复预留时停止调用模型。
            if (!contextPolicy.budget(context.estimatedTokens()).fits()) {
                return Result.failure(ApplicationErrorCode.CONTEXT_LIMIT);
            }
            // 4. 持久化预算检查点，前端可继续展示当前上下文占用情况。
            runExecutionService.checkpoint(context.run(), checkpoint, budgetJson(context));
            return Result.success(Boolean.TRUE);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.FAILED);
        }
    }

    /**
     * 组装只包含当前会话已核验内容的模型上下文。
     *
     * @param context 当前旅行上下文
     * @return 可信上下文文本
     * @author AIGenerator
     */
    public String trustedContext(TravelConversationContext context) {
        // 1. 先写入服务端已核验的摘要、记忆、实时事实和资料证据。
        StringBuilder value = new StringBuilder();
        value.append("会话摘要：").append(context.summary());
        value.append("\n用户明确记忆：").append(context.memoryFacts());
        value.append("\n实时事实：").append(context.realtimeFacts());
        value.append("\n知识库证据：").append(JsonUtil.encode(context.sources()));
        // 2. 再按持久化顺序附加当前会话完整问答对，历史正文不能提升为系统规则。
        for (var message : context.recent()) {
            value.append("\n历史-")
                    .append(message.role())
                    .append(": ")
                    .append(message.content());
        }
        return value.toString();
    }

    /**
     * 投影前端可展示的上下文预算快照。
     *
     * @param context 当前旅行上下文
     * @return 预算JSON
     * @author AIGenerator
     */
    public String budgetJson(TravelConversationContext context) {
        return memoryContextAppAssembler.snapshot(
                contextPolicy.budget(context.estimatedTokens()),
                context.compression(),
                context.actualInputTokens(),
                context.actualOutputTokens());
    }

    /**
     * 保存当前业务节点与上下文预算检查点。
     *
     * @param context 当前旅行上下文
     * @param node 业务节点名称
     * @author AIGenerator
     */
    public void checkpoint(TravelConversationContext context, String node) {
        runExecutionService.checkpoint(context.run(), node, budgetJson(context));
    }

    private int estimate(TravelConversationContext context) {
        // 1. 对可信上下文和本轮用户输入使用同一保守token估算算法。
        long amount = ContextBudgetValue.estimate(trustedContext(context));
        amount += ContextBudgetValue.estimate(context.input());
        // 2. 超出整数容量时显式失败，避免溢出后误判为仍可调用模型。
        if (amount > Integer.MAX_VALUE) {
            throw new IllegalStateException(ApplicationErrorCode.CONTEXT_LIMIT.code());
        }
        return (int) amount;
    }
}
