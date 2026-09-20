package com.hellotravel.adaptor.chat.output;

import com.hellotravel.adaptor.chat.output.aiservice.TravelIntentAiService;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.adaptor.TravelIntentRecognitionAdaptor;
import com.hellotravel.application.chat.command.TravelIntentCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LangChain4j对旅行意图识别端口的实现。
 *
 * @author AIGenerator
 */
@Component
public final class TravelIntentRecognitionAdaptorImpl implements TravelIntentRecognitionAdaptor {

    private final TravelIntentAiService travelIntentAiService;
    private final ChatContextPolicy chatContextPolicy;

    public TravelIntentRecognitionAdaptorImpl(
            TravelIntentAiService travelIntentAiService,
            ChatContextPolicy chatContextPolicy) {
        this.travelIntentAiService = travelIntentAiService;
        this.chatContextPolicy = chatContextPolicy;
    }

    /**
     * 识别旅行意图并转换为框架无关模型。
     *
     * @param travelIntentCommand 意图识别命令
     * @return 结构化旅行意图
     * @author AIGenerator
     */
    @Override
    public Result<TravelIntentDO> recognize(TravelIntentCommand travelIntentCommand) {
        try {
            // 1. 调用模型前核验可信规则和用户正文的完整预算。
            if (!chatContextPolicy.accepts(
                    travelIntentCommand.instructions(),
                    List.of(travelIntentCommand.input()),
                    ChatModelStage.INTENT)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            // 2. 由LangChain4j直接转换为框架无关的结构化意图模型。
            TravelIntentDO intent = travelIntentAiService.recognize(
                    travelIntentCommand.input(), travelIntentCommand.instructions());
            if (intent == null) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            return Result.success(intent);
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
