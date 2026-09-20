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
 * 核对规划所需日期、人数、目的地和一期服务边界。
 *
 * @author AIGenerator
 */
@Component
public final class ValidateTravelRequestNode {

    private final TravelPlanningContextRegistry travelPlanningContextRegistry;
    private final TravelPlanDomainService travelPlanDomainService;
    private final TravelContextService travelContextService;
    private final TravelPlanAppAssembler travelPlanAppAssembler;

    public ValidateTravelRequestNode(
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
            // 1. 由确定性领域规则核对需求，不让模型猜测缺失的关键字段。
            var context = travelPlanningContextRegistry.require(state.contextKey());
            var validation = travelPlanDomainService.validateRequest(
                    travelPlanAppAssembler.requestValidation(context));
            if (!validation.success()) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 保存结构化校验结果，条件边决定继续规划或向用户追问。
            context.validation(validation.data());
            travelContextService.checkpoint(context, TravelPlanningNode.VALIDATE_REQUEST.key());
            return Map.of(TravelPlanningState.CONTEXT_KEY, state.contextKey());
        } catch (ApplicationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApplicationException(ApplicationErrorCode.FAILED);
        }
    }
}
