package com.hellotravel.adaptor.chat.output;

import com.hellotravel.adaptor.chat.output.aiservice.TravelDialogueAiService;
import com.hellotravel.adaptor.chat.output.converter.ChatModelConverter;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.adaptor.TravelDialogueAdaptor;
import com.hellotravel.application.chat.command.TravelDialogueCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * LangChain4j对普通旅行对话端口的流式实现。
 *
 * @author AIGenerator
 */
@Component
public final class TravelDialogueAdaptorImpl implements TravelDialogueAdaptor {

    private final TravelDialogueAiService travelDialogueAiService;
    private final ChatContextPolicy chatContextPolicy;
    private final ChatModelConverter chatModelConverter;
    private final Environment environment;

    public TravelDialogueAdaptorImpl(
            TravelDialogueAiService travelDialogueAiService,
            ChatContextPolicy chatContextPolicy,
            ChatModelConverter chatModelConverter,
            Environment environment) {
        this.travelDialogueAiService = travelDialogueAiService;
        this.chatContextPolicy = chatContextPolicy;
        this.chatModelConverter = chatModelConverter;
        this.environment = environment;
    }

    /**
     * 流式生成普通旅行对话回答。
     *
     * @param travelDialogueCommand 普通对话命令
     * @return 完整模型回答
     * @author AIGenerator
     */
    @Override
    public Result<ChatModelDO> answer(TravelDialogueCommand travelDialogueCommand) {
        try {
            // 1. 在计费调用前核验规则、上下文和本轮输入的完整预算。
            if (!chatContextPolicy.accepts(
                    travelDialogueCommand.instructions(),
                    List.of(travelDialogueCommand.context(), travelDialogueCommand.input()),
                    ChatModelStage.DIALOGUE)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            // 2. 累积流式正文并把节流后的完整草稿交给应用层进度回调。
            StringBuilder text = new StringBuilder();
            travelDialogueAiService.answer(
                            travelDialogueCommand.input(),
                            travelDialogueCommand.instructions(),
                            travelDialogueCommand.context())
                    .doOnNext(
                            chunk -> {
                                // 1. 累积当前流式片段，形成可持久化的完整草稿。
                                text.append(chunk);
                                // 2. 超长或租约回调拒绝时中止上游，避免无归属生成继续计费。
                                if (text.length() > 262_144
                                        || (travelDialogueCommand.partial() != null
                                                && !travelDialogueCommand
                                                        .partial()
                                                        .test(text.toString()))) {
                                    throw new CancellationException("travel dialogue cancelled");
                                }
                            })
                    .then()
                    .block(Duration.ofSeconds(95));
            // 3. 空流不能提交为成功回答；供应商未返回usage时保持未知。
            if (text.isEmpty()) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            return Result.success(chatModelConverter.response(
                    text.toString(), environment.getProperty("CHAT_MODEL", "qwen-plus")));
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
