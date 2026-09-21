package com.hellotravel.adaptor.chat.output;

import com.hellotravel.adaptor.chat.output.aiservice.TravelPlanAiService;
import com.hellotravel.adaptor.chat.output.converter.TravelPlanConverter;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.adaptor.TravelPlanGenerationAdaptor;
import com.hellotravel.application.chat.command.TravelPlanGenerationCommand;
import com.hellotravel.application.chat.command.TravelPlanRevisionCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelPlanGenerationDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * LangChain4j对旅行计划草稿生成端口的结构化实现。
 *
 * @author AIGenerator
 */
@Component
public final class TravelPlanGenerationAdaptorImpl implements TravelPlanGenerationAdaptor {

    /**
     * 记录不包含提示词和密钥的模型调用失败证据。
     *
     * @author AIGenerator
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TravelPlanGenerationAdaptorImpl.class);

    private final TravelPlanAiService travelPlanAiService;
    private final ChatContextPolicy chatContextPolicy;
    private final TravelPlanConverter travelPlanConverter;
    private final String modelName;

    public TravelPlanGenerationAdaptorImpl(
            TravelPlanAiService travelPlanAiService,
            ChatContextPolicy chatContextPolicy,
            TravelPlanConverter travelPlanConverter,
            @Value("\u0024{langchain4j.open-ai.chat-model.model-name}") String modelName) {
        this.travelPlanAiService = travelPlanAiService;
        this.chatContextPolicy = chatContextPolicy;
        this.travelPlanConverter = travelPlanConverter;
        this.modelName = modelName;
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
            return Result.success(travelPlanConverter.generation(
                    draft, modelName));
        } catch (Exception exception) {
            // 3. 记录阶段和根因供运行排障，正文与认证信息不进入日志。
            LOGGER.warn(
                    "travel_plan_model_failed stage={} cause_type={}",
                    ChatModelStage.PLAN_DRAFT,
                    exception.getClass().getName(),
                    exception);
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
            return Result.success(travelPlanConverter.generation(
                    draft, modelName));
        } catch (Exception exception) {
            // 3. 记录阶段和根因供运行排障，正文与认证信息不进入日志。
            LOGGER.warn(
                    "travel_plan_model_failed stage={} cause_type={}",
                    ChatModelStage.PLAN_REVISION,
                    exception.getClass().getName(),
                    exception);
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
