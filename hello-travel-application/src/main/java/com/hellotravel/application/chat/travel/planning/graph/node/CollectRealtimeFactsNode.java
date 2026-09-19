package com.hellotravel.application.chat.travel.planning.graph.node;

import com.hellotravel.application.chat.travel.context.TravelContextService;
import com.hellotravel.application.chat.travel.fact.evidence.TravelEvidenceCollector;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningState;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.planning.graph.TravelPlanningNode;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 查询天气和路线等有时效性的外部事实。
 *
 * @author AIGenerator
 */
@Component
public final class CollectRealtimeFactsNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelEvidenceCollector travelEvidenceService;
    private final TravelContextService travelContextService;

    public CollectRealtimeFactsNode(
            TravelEvidenceCollector travelEvidenceService,
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
            // 1. 只按结构化意图调用固定外部端点，模型不能提供任意URL。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            if (!travelEvidenceService.collectRealtimeFacts(context).success()) {
                throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
            }
            // 2. 将外部事实纳入预算后再进入草稿生成阶段。
            ApplicationFailures.required(
                    travelContextService.ensureBudget(
                            context, TravelPlanningNode.COLLECT_REALTIME_FACTS.key()));
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
    }
}
