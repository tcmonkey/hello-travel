package com.hellotravel.adaptor.chat.output.planning;

import com.hellotravel.adaptor.chat.output.planning.converter.TravelPlanOutputConverter;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.travel.planning.adaptor.TravelPlanGenerationAdaptor;
import com.hellotravel.application.chat.travel.planning.command.TravelPlanGenerationCommand;
import com.hellotravel.application.chat.travel.planning.command.TravelPlanRevisionCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelPlanGenerationDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j对旅行计划草稿生成端口的结构化实现。
 *
 * @author AIGenerator
 */
@Component
public final class LangChain4jTravelPlanGenerationOutAdaptor
        implements TravelPlanGenerationAdaptor {

    private final TravelPlanAiService travelPlanAiService;
    private final ChatContextPolicy chatContextPolicy;
    private final TravelPlanOutputConverter travelPlanOutputConverter;
    private final Environment environment;

    public LangChain4jTravelPlanGenerationOutAdaptor(
            TravelPlanAiService travelPlanAiService,
            ChatContextPolicy chatContextPolicy,
            TravelPlanOutputConverter travelPlanOutputConverter,
            Environment environment) {
        this.travelPlanAiService = travelPlanAiService;
        this.chatContextPolicy = chatContextPolicy;
        this.travelPlanOutputConverter = travelPlanOutputConverter;
        this.environment = environment;
    }

    /**
     * 生成结构化旅行计划草稿。
     *
     * @param travelPlanGenerationCommand 草稿生成命令
     * @return 草稿和模型证据
     * @author AIGenerator
     */
    @Override
    public Result<TravelPlanGenerationDO> generate(
            TravelPlanGenerationCommand travelPlanGenerationCommand) {
        try {
            // 1. 核验可信规则、需求和证据的完整预算。
            if (!chatContextPolicy.accepts(
                    travelPlanGenerationCommand.instructions(),
                    List.of(
                            travelPlanGenerationCommand.requirements(),
                            travelPlanGenerationCommand.evidence()),
                    ChatModelStage.PLAN_DRAFT)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            // 2. 调用结构化AiService，返回不含任何LangChain4j类型的草稿。
            var draft = travelPlanAiService.generate(
                    travelPlanGenerationCommand.requirements(),
                    travelPlanGenerationCommand.evidence(),
                    travelPlanGenerationCommand.instructions());
            if (draft == null) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            return Result.success(travelPlanOutputConverter.generation(
                    draft, environment.getProperty("CHAT_MODEL", "qwen-plus")));
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 根据领域违规项修订结构化旅行计划草稿。
     *
     * @param travelPlanRevisionCommand 草稿修订命令
     * @return 修订草稿和模型证据
     * @author AIGenerator
     */
    @Override
    public Result<TravelPlanGenerationDO> revise(
            TravelPlanRevisionCommand travelPlanRevisionCommand) {
        try {
            // 1. 核验需求、证据、草稿和违规项，避免修订调用突破上下文预算。
            if (!chatContextPolicy.accepts(
                    travelPlanRevisionCommand.instructions(),
                    List.of(
                            travelPlanRevisionCommand.requirements(),
                            travelPlanRevisionCommand.evidence(),
                            travelPlanRevisionCommand.draft(),
                            travelPlanRevisionCommand.violations()),
                    ChatModelStage.PLAN_REVISION)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            // 2. 只把领域规则给出的违规项交给模型定向修订。
            var draft = travelPlanAiService.revise(
                    travelPlanRevisionCommand.requirements(),
                    travelPlanRevisionCommand.evidence(),
                    travelPlanRevisionCommand.draft(),
                    travelPlanRevisionCommand.violations(),
                    travelPlanRevisionCommand.instructions());
            if (draft == null) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            return Result.success(travelPlanOutputConverter.generation(
                    draft, environment.getProperty("CHAT_MODEL", "qwen-plus")));
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
