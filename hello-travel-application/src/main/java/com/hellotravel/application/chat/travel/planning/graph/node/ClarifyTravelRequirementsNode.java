package com.hellotravel.application.chat.travel.planning.graph.node;

import com.hellotravel.application.chat.travel.context.TravelContextService;
import com.hellotravel.application.chat.travel.planning.assembler.TravelPlanAssembler;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningState;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningNode;
import com.hellotravel.model.chat.ChatModelDO;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 在缺少关键信息或有界修订仍失败时返回明确补充问题。
 *
 * @author AIGenerator
 */
@Component
public final class ClarifyTravelRequirementsNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanAssembler travelPlanAssembler;
    private final TravelContextService travelContextService;

    public ClarifyTravelRequirementsNode(
            TravelPlanAssembler travelPlanAssembler,
            TravelContextService travelContextService,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelPlanAssembler = travelPlanAssembler;
        this.travelContextService = travelContextService;
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
            // 1. 根据结构化问题或违规项生成确定性追问，不为追问额外调用模型。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            context.answer(travelPlanAssembler.clarify(context));
            if (context.modelResponse() == null) {
                context.modelResponse(new ChatModelDO("", null, null, null, "deterministic"));
            }
            // 2. 保存追问节点，用户补充后将作为下一轮重新识别和规划。
            travelContextService.checkpoint(
                    context, TravelPlanningNode.CLARIFY_REQUIREMENTS.key());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (Exception exception) {
            throw new IllegalStateException("clarify requirements failed", exception);
        }
    }
}
