package com.hellotravel.application.chat.travel;

import com.hellotravel.application.chat.support.TravelConversationContext;
import com.hellotravel.application.chat.travel.node.ClarifyTravelRequirementsNode;
import com.hellotravel.application.chat.travel.node.CollectDestinationEvidenceNode;
import com.hellotravel.application.chat.travel.node.CollectRealtimeFactsNode;
import com.hellotravel.application.chat.travel.node.GenerateTravelDraftNode;
import com.hellotravel.application.chat.travel.node.RenderTravelPlanNode;
import com.hellotravel.application.chat.travel.node.ReviseTravelDraftNode;
import com.hellotravel.application.chat.travel.node.ValidateTravelDraftNode;
import com.hellotravel.application.chat.travel.node.ValidateTravelRequestNode;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 旅行计划应用编排图；节点可调用领域服务、仓储能力和外部防腐端口。
 *
 * @author AIGenerator
 */
@Component
public final class TravelPlanningGraph {

    private final ValidateTravelRequestNode validateTravelRequestNode;
    private final CollectDestinationEvidenceNode collectDestinationEvidenceNode;
    private final CollectRealtimeFactsNode collectRealtimeFactsNode;
    private final GenerateTravelDraftNode generateTravelDraftNode;
    private final ValidateTravelDraftNode validateTravelDraftNode;
    private final ReviseTravelDraftNode reviseTravelDraftNode;
    private final RenderTravelPlanNode renderTravelPlanNode;
    private final ClarifyTravelRequirementsNode clarifyTravelRequirementsNode;
    private final TravelPlanningContextRegistry travelPlanningContextRegistry;

    public TravelPlanningGraph(
            ValidateTravelRequestNode validateTravelRequestNode,
            CollectDestinationEvidenceNode collectDestinationEvidenceNode,
            CollectRealtimeFactsNode collectRealtimeFactsNode,
            GenerateTravelDraftNode generateTravelDraftNode,
            ValidateTravelDraftNode validateTravelDraftNode,
            ReviseTravelDraftNode reviseTravelDraftNode,
            RenderTravelPlanNode renderTravelPlanNode,
            ClarifyTravelRequirementsNode clarifyTravelRequirementsNode,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.validateTravelRequestNode = validateTravelRequestNode;
        this.collectDestinationEvidenceNode = collectDestinationEvidenceNode;
        this.collectRealtimeFactsNode = collectRealtimeFactsNode;
        this.generateTravelDraftNode = generateTravelDraftNode;
        this.validateTravelDraftNode = validateTravelDraftNode;
        this.reviseTravelDraftNode = reviseTravelDraftNode;
        this.renderTravelPlanNode = renderTravelPlanNode;
        this.clarifyTravelRequirementsNode = clarifyTravelRequirementsNode;
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
    }

    /**
     * 执行旅行规划业务图。
     *
     * @param context 已准备并识别意图的旅行上下文
     * @return 完成计划或追问的上下文
     * @author AIGenerator
     */
    public Result<TravelConversationContext> execute(TravelConversationContext context) {
        // 1. 预留本次执行键，确保成功、失败和异常终止都能在finally清理。
        String contextKey = null;
        // 2. 在图执行异常边界内完成注册、编排、执行和业务结果转换。
        try {
            // 1. 登记业务上下文，图状态只保存可安全克隆的执行键。
            contextKey = travelPlanningContextRegistry.register(context);
            // 2. 构建并执行无共享可变状态的有界图，单次状态只属于当前调用。
            var result = compileGraph().invoke(
                    Map.of(TravelPlanningState.CONTEXT_KEY, contextKey));
            if (result.isEmpty()) {
                return Result.failure(ApplicationErrorCode.FAILED);
            }
            return Result.success(context);
        } catch (Exception exception) {
            return Failures.capture(exception, ApplicationErrorCode.FAILED);
        } finally {
            if (contextKey != null) {
                travelPlanningContextRegistry.remove(contextKey);
            }
        }
    }

    /**
     * 按稳定节点定义构建当前旅行规划图。
     *
     * @return 已校验并编译的无检查点状态图
     * @author AIGenerator
     */
    private org.bsc.langgraph4j.CompiledGraph<TravelPlanningState> compileGraph() {
        try {
            // 1. 使用统一节点枚举注册节点，持久化检查点与Graph名称保持一致。
            StateGraph<TravelPlanningState> graph = new StateGraph<>(TravelPlanningState::new);
            graph.addNode(
                    TravelPlanningNode.VALIDATE_REQUEST.key(),
                    AsyncNodeAction.node_async(validateTravelRequestNode::execute));
            graph.addNode(
                    TravelPlanningNode.COLLECT_DESTINATION_EVIDENCE.key(),
                    AsyncNodeAction.node_async(collectDestinationEvidenceNode::execute));
            graph.addNode(
                    TravelPlanningNode.COLLECT_REALTIME_FACTS.key(),
                    AsyncNodeAction.node_async(collectRealtimeFactsNode::execute));
            graph.addNode(
                    TravelPlanningNode.GENERATE_DRAFT.key(),
                    AsyncNodeAction.node_async(generateTravelDraftNode::execute));
            graph.addNode(
                    TravelPlanningNode.VALIDATE_DRAFT.key(),
                    AsyncNodeAction.node_async(validateTravelDraftNode::execute));
            graph.addNode(
                    TravelPlanningNode.REVISE_DRAFT.key(),
                    AsyncNodeAction.node_async(reviseTravelDraftNode::execute));
            graph.addNode(
                    TravelPlanningNode.RENDER_PLAN.key(),
                    AsyncNodeAction.node_async(renderTravelPlanNode::execute));
            graph.addNode(
                    TravelPlanningNode.CLARIFY_REQUIREMENTS.key(),
                    AsyncNodeAction.node_async(clarifyTravelRequirementsNode::execute));
            // 2. 需求缺失直接追问；信息完整后依次收集证据和实时事实。
            graph.addEdge(StateGraph.START, TravelPlanningNode.VALIDATE_REQUEST.key());
            graph.addConditionalEdges(
                    TravelPlanningNode.VALIDATE_REQUEST.key(),
                    AsyncEdgeAction.edge_async(
                            state -> travelPlanningContextRegistry
                                    .require(state.contextKey())
                                    .validation()
                                    .valid()
                                    ? "continue"
                                    : "clarify"),
                    Map.of(
                            "continue",
                            TravelPlanningNode.COLLECT_DESTINATION_EVIDENCE.key(),
                            "clarify",
                            TravelPlanningNode.CLARIFY_REQUIREMENTS.key()));
            graph.addEdge(
                    TravelPlanningNode.COLLECT_DESTINATION_EVIDENCE.key(),
                    TravelPlanningNode.COLLECT_REALTIME_FACTS.key());
            graph.addEdge(
                    TravelPlanningNode.COLLECT_REALTIME_FACTS.key(),
                    TravelPlanningNode.GENERATE_DRAFT.key());
            graph.addEdge(
                    TravelPlanningNode.GENERATE_DRAFT.key(),
                    TravelPlanningNode.VALIDATE_DRAFT.key());
            // 3. 草稿通过则输出；可修订时最多循环两次，其余情况停止并明确说明。
            graph.addConditionalEdges(
                    TravelPlanningNode.VALIDATE_DRAFT.key(),
                    AsyncEdgeAction.edge_async(this::routeAfterDraftValidation),
                    Map.of(
                            "render",
                            TravelPlanningNode.RENDER_PLAN.key(),
                            "revise",
                            TravelPlanningNode.REVISE_DRAFT.key(),
                            "clarify",
                            TravelPlanningNode.CLARIFY_REQUIREMENTS.key()));
            graph.addEdge(
                    TravelPlanningNode.REVISE_DRAFT.key(),
                    TravelPlanningNode.VALIDATE_DRAFT.key());
            graph.addEdge(TravelPlanningNode.RENDER_PLAN.key(), StateGraph.END);
            graph.addEdge(TravelPlanningNode.CLARIFY_REQUIREMENTS.key(), StateGraph.END);
            // 4. 完成结构校验与编译，图本身不持有当前用户业务上下文。
            return graph.compile(CompileConfig.builder().recursionLimit(16).build());
        } catch (Exception exception) {
            throw new IllegalStateException("travel planning graph compilation failed", exception);
        }
    }

    private String routeAfterDraftValidation(TravelPlanningState state) {
        // 1. 已通过领域校验的草稿直接进入确定性输出节点。
        var context = travelPlanningContextRegistry.require(state.contextKey());
        if (context.validation().valid()) {
            return "render";
        }
        // 2. 仍可修订且未达到上限时只循环到定向修订节点。
        if (context.validation().revisable() && context.revisionCount() < 2) {
            return "revise";
        }
        // 3. 不可修订或达到上限时停止循环并向用户说明缺口。
        return "clarify";
    }
}
