package com.hellotravel.application.chat.travel.node;

import com.hellotravel.application.exception.RunExecutionService;
import com.hellotravel.application.chat.adaptor.TravelPlanGenerationAdaptor;
import com.hellotravel.application.chat.assembler.TravelPlanAppAssembler;
import com.hellotravel.application.chat.travel.TravelPlanningState;
import com.hellotravel.application.chat.travel.TravelPlanningContextRegistry;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 按领域违规项执行最多两次的定向草稿修订。
 *
 * @author AIGenerator
 */
@Component
public final class ReviseTravelDraftNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanGenerationAdaptor travelPlanGenerationAdaptor;
    private final TravelPlanAppAssembler travelPlanAppAssembler;
    private final RunExecutionService runExecutionService;

    public ReviseTravelDraftNode(
            TravelPlanGenerationAdaptor travelPlanGenerationAdaptor,
            TravelPlanAppAssembler travelPlanAppAssembler,
            RunExecutionService runExecutionService,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelPlanGenerationAdaptor = travelPlanGenerationAdaptor;
        this.travelPlanAppAssembler = travelPlanAppAssembler;
        this.runExecutionService = runExecutionService;
    }

    /**
     * 执行当前旅行规划节点。
     *
     * @param state 当前规划图状态
     * @return 更新后的图状态
     * @author AIGenerator
     */
    public Map<String, Object> execute(TravelPlanningState state) {
        try {
            var context = travelPlanningContextRegistry.require(state.contextKey());
            // 1. 增加修订次数并登记独立模型调用，Graph条件边负责限制最大次数。
            int revision = context.revisionCount() + 1;
            String invocationId = runExecutionService.invocation(
                    context.run(),
                    ChatModelStage.PLAN_REVISION.name(),
                    revision,
                    context.estimatedTokens());
            long started = System.nanoTime();
            var response = travelPlanGenerationAdaptor.revise(
                    travelPlanAppAssembler.revision(context));
            var generation = response != null && response.success() ? response.data() : null;
            boolean complete = generation != null
                    && generation.draft() != null
                    && generation.model() != null;
            runExecutionService.invocationComplete(
                    invocationId,
                    generation == null ? null : generation.model(),
                    (System.nanoTime() - started) / 1_000_000,
                    complete);
            // 2. 用新草稿替换旧草稿，再回到同一个确定性校验节点。
            if (response == null || (response.success() && response.data() == null)) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
            ApplicationFailures.required(response);
            if (!complete) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
            context.revisionCount(revision);
            context.draft(generation.draft());
            context.modelResponse(generation.model());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
    }
}
