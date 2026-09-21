package com.hellotravel.application.chat.travel;

import com.hellotravel.application.chat.assembler.TravelAppAssembler;
import com.hellotravel.application.chat.command.TravelGenerateCommand;
import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.chat.TravelDialogueAppService;
import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.chat.adaptor.TravelIntentRecognitionAdaptor;
import com.hellotravel.application.chat.assembler.TravelIntentAppAssembler;
import com.hellotravel.application.chat.result.TravelGenerateAppResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.util.JsonUtil;
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
public final class TravelAppService {

    private final RunExecutionService runExecutionService;
    private final TravelContextService travelContextService;
    private final TravelIntentRecognitionAdaptor travelIntentRecognitionAdaptor;
    private final TravelIntentAppAssembler travelIntentAppAssembler;
    private final TravelDialogueAppService travelDialogueService;
    private final TravelPlanningGraph travelPlanningGraph;
    private final TravelAppAssembler travelAppAssembler;

    public TravelAppService(
            RunExecutionService runExecutionService,
            TravelContextService travelContextService,
            TravelIntentRecognitionAdaptor travelIntentRecognitionAdaptor,
            TravelIntentAppAssembler travelIntentAppAssembler,
            TravelDialogueAppService travelDialogueService,
            TravelPlanningGraph travelPlanningGraph,
            TravelAppAssembler travelAppAssembler) {
        this.runExecutionService = runExecutionService;
        this.travelContextService = travelContextService;
        this.travelIntentRecognitionAdaptor = travelIntentRecognitionAdaptor;
        this.travelIntentAppAssembler = travelIntentAppAssembler;
        this.travelDialogueService = travelDialogueService;
        this.travelPlanningGraph = travelPlanningGraph;
        this.travelAppAssembler = travelAppAssembler;
    }

    /**
     * 执行已由HTTP用例持久化并通过Outbox投递的旅行回答任务。
     *
     * @param travelGenerateCommand 后台生成命令
     * @return 生成任务处理结果
     * @author AIGenerator
     */
    public Result<TravelGenerateAppResult> generate(TravelGenerateCommand travelGenerateCommand) {
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
                return Result.success(travelAppAssembler.completed());
            } catch (Exception exception) {
                // 1. 已领取任务保留现有草稿并转换为明确失败终态。
                Result<TravelGenerateAppResult> failure = ApplicationFailures.capture(exception);
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
                travelIntentAppAssembler.command(context));
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
        // 1. 已具备计划关键槽位时优先进入规划，防止模型将补充信息错误降级为闲聊。
        if (hasPlanningDetails(intent)) {
            return TravelIntentMode.PLANNING;
        }
        // 2. 采用结构化模型返回的受限枚举，不在调用链散落字符串动作。
        if (intent.mode() != null) {
            return intent.mode();
        }
        // 3. 无明确模式时仅有实时事实标记进入事实问答，其余保持普通对话。
        return intent.weather() || intent.route() || intent.knowledge()
                ? TravelIntentMode.FACT_QUERY
                : TravelIntentMode.DIALOGUE;
    }

    private boolean hasPlanningDetails(TravelIntentDO intent) {
        // 1. 日期或游玩天数表明用户已给出可编排行程的时间范围。
        boolean hasSchedule = intent.days() != null
                || intent.startDate() != null
                || intent.endDate() != null;
        // 2. 目的地或人数至少有一项时，结构化结果已具备继续规划而非闲聊的业务语义。
        boolean hasTravelSubject = (intent.destination() != null && !intent.destination().isBlank())
                || intent.travelers() != null;
        // 3. 只有时间和旅行主体同时存在才强制进入计划图，避免普通日期问答被误路由。
        return hasSchedule && hasTravelSubject;
    }

    private void finalizeAnswer(TravelConversationContext context) {
        // 1. 将已核验来源投影为前端引用，不把知识块正文写入消息引用字段。
        String citations = JsonUtil.encode(
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
