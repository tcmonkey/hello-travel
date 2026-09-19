package com.hellotravel.application.chat.travel.planning.graph.node;

import com.hellotravel.application.chat.travel.context.TravelConversationContext;
import com.hellotravel.application.chat.travel.execution.RunExecutionService;
import com.hellotravel.application.chat.travel.planning.adaptor.TravelPlanGenerationAdaptor;
import com.hellotravel.application.chat.travel.planning.assembler.TravelPlanAssembler;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningState;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningContextRegistry;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基于结构化需求和已核验证据生成第一版计划草稿。
 *
 * @author AIGenerator
 */
@Component
public final class GenerateTravelDraftNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanGenerationAdaptor travelPlanGenerationAdaptor;
    private final TravelPlanAssembler travelPlanAssembler;
    private final RunExecutionService runExecutionService;

    public GenerateTravelDraftNode(
            TravelPlanGenerationAdaptor travelPlanGenerationAdaptor,
            TravelPlanAssembler travelPlanAssembler,
            RunExecutionService runExecutionService,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelPlanGenerationAdaptor = travelPlanGenerationAdaptor;
        this.travelPlanAssembler = travelPlanAssembler;
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
        // 1. 准备本节点共享的业务上下文、调用标识与计时起点。
        TravelConversationContext context = travelPlanningContextRegistry.require(state.contextKey());
        String invocationId = null;
        long started = System.nanoTime();
        // 2. 在受控异常边界内执行模型调用，失败统一转换为应用层不可用。
        try {
            // 1. 先登记模型调用证据，再调用草稿生成防腐端口。
            invocationId = runExecutionService.invocation(
                    context.run(),
                    ChatModelStage.PLAN_DRAFT.name(),
                    1,
                    context.estimatedTokens());
            var response = travelPlanGenerationAdaptor.generate(
                    travelPlanAssembler.generation(context));
            var generation = response != null && response.success() ? response.data() : null;
            boolean complete = generation != null
                    && generation.draft() != null
                    && generation.model() != null;
            runExecutionService.invocationComplete(
                    invocationId,
                    generation == null ? null : generation.model(),
                    (System.nanoTime() - started) / 1_000_000,
                    complete);
            // 2. 只有结构化草稿与模型证据同时成功时才交给领域规则校验。
            if (response == null || (response.success() && response.data() == null)) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
            ApplicationFailures.required(response);
            if (!complete) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
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
