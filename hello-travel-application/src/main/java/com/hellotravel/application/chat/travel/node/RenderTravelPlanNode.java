package com.hellotravel.application.chat.travel.node;

import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.assembler.TravelPlanAppAssembler;
import com.hellotravel.application.chat.travel.TravelPlanningState;
import com.hellotravel.application.chat.travel.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.TravelPlanningNode;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 将已通过领域校验的结构化计划确定性渲染为用户答案。
 *
 * @author AIGenerator
 */
@Component
public final class RenderTravelPlanNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanAppAssembler travelPlanAppAssembler;
    private final TravelContextService travelContextService;

    public RenderTravelPlanNode(
            TravelPlanAppAssembler travelPlanAppAssembler,
            TravelContextService travelContextService,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelPlanAppAssembler = travelPlanAppAssembler;
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
            // 1. 只渲染已验证草稿，不再次调用模型改变计划事实。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            context.answer(travelPlanAppAssembler.render(context));
            // 2. 保存最终规划节点，回答由统一运行服务原子落库。
            travelContextService.checkpoint(context, TravelPlanningNode.RENDER_PLAN.key());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (Exception exception) {
            throw new IllegalStateException("render plan failed", exception);
        }
    }
}
