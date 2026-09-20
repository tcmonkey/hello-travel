package com.hellotravel.application.chat.travel.node;

import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.TravelEvidenceCollectorAppService;
import com.hellotravel.application.chat.travel.TravelPlanningState;
import com.hellotravel.application.chat.travel.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.TravelPlanningNode;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 召回用户知识库中的景区、退改与注意事项证据。
 *
 * @author AIGenerator
 */
@Component
public final class CollectDestinationEvidenceNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelEvidenceCollectorAppService travelEvidenceService;
    private final TravelContextService travelContextService;

    public CollectDestinationEvidenceNode(
            TravelEvidenceCollectorAppService travelEvidenceService,
            TravelContextService travelContextService,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelEvidenceService = travelEvidenceService;
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
            // 1. 查询可追溯资料；不可用时清空来源，禁止模型伪造政策引用。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            if (!travelEvidenceService.collectDestinationEvidence(context).success()) {
                context.sources(List.of());
            }
            // 2. 记录证据阶段检查点，正文仍不写入图运行日志。
            travelContextService.checkpoint(
                    context, TravelPlanningNode.COLLECT_DESTINATION_EVIDENCE.key());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (Exception exception) {
            throw new IllegalStateException("destination evidence failed", exception);
        }
    }
}
