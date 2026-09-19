package com.hellotravel.application.chat.travel.dialogue.flow;

import com.hellotravel.application.chat.travel.context.TravelContextService;
import com.hellotravel.application.chat.travel.context.TravelConversationContext;
import com.hellotravel.application.chat.travel.dialogue.adaptor.TravelDialogueAdaptor;
import com.hellotravel.application.chat.travel.dialogue.assembler.TravelDialogueAssembler;
import com.hellotravel.application.chat.travel.execution.RunExecutionService;
import com.hellotravel.application.chat.travel.fact.evidence.TravelEvidenceCollector;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 普通旅行对话与事实问答用例；不进入规划Graph。
 *
 * @author AIGenerator
 */
@Component
public final class TravelDialogueFlow {

    private final TravelDialogueAdaptor travelDialogueAdaptor;
    private final TravelDialogueAssembler travelDialogueAssembler;
    private final TravelEvidenceCollector travelEvidenceService;
    private final TravelContextService travelContextService;
    private final RunExecutionService runExecutionService;

    public TravelDialogueFlow(
            TravelDialogueAdaptor travelDialogueAdaptor,
            TravelDialogueAssembler travelDialogueAssembler,
            TravelEvidenceCollector travelEvidenceService,
            TravelContextService travelContextService,
            RunExecutionService runExecutionService) {
        this.travelDialogueAdaptor = travelDialogueAdaptor;
        this.travelDialogueAssembler = travelDialogueAssembler;
        this.travelEvidenceService = travelEvidenceService;
        this.travelContextService = travelContextService;
        this.runExecutionService = runExecutionService;
    }

    /**
     * 生成普通旅行对话或带外部事实的回答。
     *
     * @param context 当前旅行上下文
     * @param withFacts 是否先查询实时事实和知识库证据
     * @return 已形成回答的上下文
     * @author AIGenerator
     */
    public Result<TravelConversationContext> answer(
            TravelConversationContext context, boolean withFacts) {
        try {
            // 1. 事实问答先查询用户要求的外部事实与私有资料，普通闲聊跳过工具。
            if (withFacts) {
                ApplicationFailures.required(
                        travelEvidenceService.collectRealtimeFacts(context));
                ApplicationFailures.required(
                        travelEvidenceService.collectDestinationEvidence(context));
            }
            // 2. 将事实加入上下文后重新核验预算，再登记本次模型调用证据。
            ApplicationFailures.required(
                    travelContextService.ensureBudget(context, "dialogue_ready"));
            String invocationId = runExecutionService.invocation(
                    context.run(), ChatModelStage.DIALOGUE.name(), 1, context.estimatedTokens());
            long started = System.nanoTime();
            AtomicLong flushed = new AtomicLong(started);
            // 3. 流式调用普通对话模型，并按节流窗口持久化可恢复进度。
            var response = travelDialogueAdaptor.answer(
                    travelDialogueAssembler.command(
                            context,
                            text -> {
                                // 1. 用单调时钟判断是否达到流式持久化节流窗口。
                                long now = System.nanoTime();
                                // 2. 未达到窗口时允许上游继续，不执行额外数据库写入。
                                if (now - flushed.get() < 500_000_000L) {
                                    return true;
                                }
                                // 3. 达到窗口后更新时间并在当前租约下保存完整草稿。
                                flushed.set(now);
                                return runExecutionService.progress(context.run(), text);
                            }));
            boolean complete = response != null && response.success() && response.data() != null;
            runExecutionService.invocationComplete(
                    invocationId,
                    complete ? response.data() : null,
                    (System.nanoTime() - started) / 1_000_000,
                    complete);
            // 4. 只有完整模型结果才能进入最终落库阶段。
            if (response == null || (response.success() && response.data() == null)) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
            var model = ApplicationFailures.required(response);
            context.modelResponse(model);
            context.answer(model.text());
            return Result.success(context);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.FAILED);
        }
    }
}
