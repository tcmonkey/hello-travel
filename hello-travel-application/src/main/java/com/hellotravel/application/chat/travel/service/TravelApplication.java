package com.hellotravel.application.chat.travel.service;

import com.hellotravel.application.chat.travel.assembler.TravelApplicationAssembler;
import com.hellotravel.application.chat.travel.command.TravelGenerateCommand;
import com.hellotravel.application.chat.travel.context.TravelContextService;
import com.hellotravel.application.chat.travel.context.TravelConversationContext;
import com.hellotravel.application.chat.travel.dialogue.flow.TravelDialogueFlow;
import com.hellotravel.application.chat.travel.execution.RunExecutionService;
import com.hellotravel.application.chat.travel.intent.adaptor.TravelIntentRecognitionAdaptor;
import com.hellotravel.application.chat.travel.intent.assembler.TravelIntentAssembler;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningGraph;
import com.hellotravel.application.chat.travel.result.TravelGenerateResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.support.Json;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.chat.ChatModelStage;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelIntentMode;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 旅行回答总用例：准备上下文、识别意图，并直接分发普通对话、事实问答或规划Graph。
 *
 * @author AIGenerator
 */
@Component
public final class TravelApplication {

    private final RunExecutionService runExecutionService;
    private final TravelContextService travelContextService;
    private final TravelIntentRecognitionAdaptor travelIntentRecognitionAdaptor;
    private final TravelIntentAssembler travelIntentAssembler;
    private final TravelDialogueFlow travelDialogueService;
    private final TravelPlanningGraph travelPlanningGraph;
    private final TravelApplicationAssembler travelApplicationAssembler;

    public TravelApplication(
            RunExecutionService runExecutionService,
            TravelContextService travelContextService,
            TravelIntentRecognitionAdaptor travelIntentRecognitionAdaptor,
            TravelIntentAssembler travelIntentAssembler,
            TravelDialogueFlow travelDialogueService,
            TravelPlanningGraph travelPlanningGraph,
            TravelApplicationAssembler travelApplicationAssembler) {
        this.runExecutionService = runExecutionService;
        this.travelContextService = travelContextService;
        this.travelIntentRecognitionAdaptor = travelIntentRecognitionAdaptor;
        this.travelIntentAssembler = travelIntentAssembler;
        this.travelDialogueService = travelDialogueService;
        this.travelPlanningGraph = travelPlanningGraph;
        this.travelApplicationAssembler = travelApplicationAssembler;
    }

    /**
     * 执行已由HTTP用例持久化并通过Outbox投递的旅行回答任务。
     *
     * @param travelGenerateCommand 后台生成命令
     * @return 生成任务处理结果
     * @author AIGenerator
     */
    public Result<TravelGenerateResult> generate(TravelGenerateCommand travelGenerateCommand) {
        try {
            // 1. 领取任务并保留栅栏快照，使后续失败能够落成明确任务终态。
            ChatRunEntity run = runExecutionService.claim(travelGenerateCommand.runId());
            // 2. 在已领取任务的异常边界内完成上下文准备、分发与最终提交。
            try {
                // 1. 加载按用户和对话隔离的历史、摘要及长期记忆。
                TravelConversationContext context = ApplicationFailures.required(
                        travelContextService.prepare(run));
                // 2. 识别一次本轮意图，应用层直接决定普通对话、事实查询或规划图。
                context.intent(recognize(context));
                TravelIntentMode mode = route(context.intent());
                TravelConversationContext completed;
                if (mode == TravelIntentMode.PLANNING) {
                    completed = ApplicationFailures.required(travelPlanningGraph.execute(context));
                } else if (mode == TravelIntentMode.FACT_QUERY) {
                    completed = ApplicationFailures.required(
                            travelDialogueService.answer(context, true));
                } else {
                    completed = ApplicationFailures.required(
                            travelDialogueService.answer(context, false));
                }
                // 3. 统一原子提交回答、引用、上下文预算与跨设备同步事件。
                finalizeAnswer(completed);
                return Result.success(travelApplicationAssembler.completed());
            } catch (Exception exception) {
                // 1. 已领取任务保留现有草稿并转换为明确失败终态。
                Result<TravelGenerateResult> failure = ApplicationFailures.capture(exception);
                runExecutionService.fail(run, failure.code());
                // 2. 返回稳定应用层错误，不把下层异常类型暴露给后台派发器。
                return failure;
            }
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }

    private TravelIntentDO recognize(TravelConversationContext context) {
        // 1. 在调用意图模型前登记估算和调用阶段。
        String invocationId = runExecutionService.invocation(
                context.run(),
                ChatModelStage.INTENT.name(),
                1,
                ContextBudgetValue.estimate(context.input()));
        long started = System.nanoTime();
        var result = travelIntentRecognitionAdaptor.recognize(
                travelIntentAssembler.command(context.input()));
        // 2. 保存成功或失败证据；结构化意图不作为模型回答正文持久化。
        boolean complete = result != null && result.success() && result.data() != null;
        runExecutionService.invocationComplete(
                invocationId,
                null,
                (System.nanoTime() - started) / 1_000_000,
                complete);
        // 3. 识别失败时停止本轮，不用字符串关键字静默猜测用户业务意图。
        if (result == null || (result.success() && result.data() == null)) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
        return ApplicationFailures.required(result);
    }

    private TravelIntentMode route(TravelIntentDO intent) {
        // 1. 优先采用结构化模型返回的受限枚举，不在调用链散落字符串动作。
        if (intent.mode() != null) {
            return intent.mode();
        }
        // 2. 有日期或天数的完整方案诉求进入规划；仅有实时事实标记进入事实问答。
        if (intent.days() != null || intent.startDate() != null || intent.endDate() != null) {
            return TravelIntentMode.PLANNING;
        }
        return intent.weather() || intent.route() || intent.knowledge()
                ? TravelIntentMode.FACT_QUERY
                : TravelIntentMode.DIALOGUE;
    }

    private void finalizeAnswer(TravelConversationContext context) {
        // 1. 将已核验来源投影为前端引用，不把知识块正文写入消息引用字段。
        String citations = Json.encode(
                context.sources().stream()
                        .map(source -> Map.of(
                                "id", source.getOrDefault("id", ""),
                                "title", source.getOrDefault("title", ""),
                                "url", source.getOrDefault("url", ""),
                                "accessedAt", source.getOrDefault("accessedAt", "")))
                        .toList());
        // 2. 确保确定性追问也有明确的非计费响应元数据。
        ChatModelDO model = context.modelResponse() == null
                ? new ChatModelDO("", null, null, null, "deterministic")
                : context.modelResponse();
        // 3. 由运行协调器完成消息、任务、同步事件和记忆提取任务的同事务提交。
        runExecutionService.finalizeAnswer(
                context.run(),
                context.answer(),
                citations,
                model,
                travelContextService.budgetJson(context));
    }
}
