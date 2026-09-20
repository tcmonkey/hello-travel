package com.hellotravel.application.chat.travel.node;

import com.hellotravel.application.chat.TravelContextService;
import com.hellotravel.application.chat.travel.TravelPlanningState;
import com.hellotravel.application.chat.travel.TravelPlanningContextRegistry;
import com.hellotravel.application.chat.travel.TravelPlanningNode;
import com.hellotravel.application.chat.assembler.TravelPlanAppAssembler;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.domain.travel.service.TravelPlanDomainService;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 以确定性规则核对计划日期、时间、接驳、预算和事实标记。
 *
 * @author AIGenerator
 */
@Component
public final class ValidateTravelDraftNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanDomainService travelPlanDomainService;
    private final TravelContextService travelContextService;
    private final TravelPlanAppAssembler travelPlanAppAssembler;

    public ValidateTravelDraftNode(
            TravelPlanDomainService travelPlanDomainService,
            TravelContextService travelContextService,
            TravelPlanAppAssembler travelPlanAppAssembler,
            TravelPlanningContextRegistry travelPlanningContextRegistry) {
        this.travelPlanningContextRegistry = travelPlanningContextRegistry;
        this.travelPlanDomainService = travelPlanDomainService;
        this.travelContextService = travelContextService;
        this.travelPlanAppAssembler = travelPlanAppAssembler;
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
            // 1. 领域服务独立于模型判断草稿是否合法，防止模型自证正确。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            var validation = travelPlanDomainService.validateDraft(
                    travelPlanAppAssembler.draftValidation(context));
            if (!validation.success()) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 保存违规项，条件边决定直接输出、有界修订或停止追问。
            context.validation(validation.data());
            travelContextService.checkpoint(context, TravelPlanningNode.VALIDATE_DRAFT.key());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationErrorCode.FAILED);
        }
    }
}
