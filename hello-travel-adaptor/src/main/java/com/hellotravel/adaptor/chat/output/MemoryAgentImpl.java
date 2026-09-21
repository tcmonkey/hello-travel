package com.hellotravel.adaptor.chat.output;

import com.hellotravel.adaptor.chat.output.aiservice.MemoryAiService;
import com.hellotravel.adaptor.chat.output.converter.ChatModelConverter;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.chat.adaptor.MemoryAgent;
import com.hellotravel.application.chat.command.MemoryAgentCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

/**
 * 会话记忆端口的 LangChain4j 高阶实现。
 *
 * @author AIGenerator
 */
@Component
public final class MemoryAgentImpl implements MemoryAgent {

    private final MemoryAiService aiService;
    private final ChatContextPolicy contextPolicy;
    private final String modelName;
    private final ChatModelConverter converter;

    public MemoryAgentImpl(
            MemoryAiService aiService,
            ChatContextPolicy contextPolicy,
            @Value("\u0024{langchain4j.open-ai.chat-model.model-name}") String modelName,
            ChatModelConverter converter) {
        this.aiService = aiService;
        this.contextPolicy = contextPolicy;
        this.modelName = modelName;
        this.converter = converter;
    }

    /**
     * 调用会话摘要Agent。
     *
     * @param memoryAgentCommand 已组装的摘要命令
     * @return 标准对话模型结果
     * @author AIGenerator
     */
    @Override
    public Result<ChatModelDO> summarize(MemoryAgentCommand memoryAgentCommand) {
        try {
            // 1. 通过固定摘要能力执行调用，不接受外部动态动作。
            return invoke(memoryAgentCommand, ChatModelStage.MEMORY_SUMMARY, aiService::summarize);
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 调用显式记忆提取Agent。
     *
     * @param memoryAgentCommand 已组装的提取命令
     * @return 标准对话模型结果
     * @author AIGenerator
     */
    @Override
    public Result<ChatModelDO> extract(MemoryAgentCommand memoryAgentCommand) {
        try {
            // 1. 通过固定提取能力执行调用，不接受外部动态动作。
            return invoke(memoryAgentCommand, ChatModelStage.MEMORY_EXTRACTION, aiService::extract);
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    private Result<ChatModelDO> invoke(
            MemoryAgentCommand command,
            ChatModelStage stage,
            BiFunction<String, String, String> operation) {
        try {
            // 1. 在计费调用前核验可信规则和记忆资料的完整上下文预算。
            if (!contextPolicy.accepts(
                    command.instructions(), List.of(command.input()), stage)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            // 2. 执行明确的记忆能力，不通过动态action选择处理分支。
            String text = operation.apply(command.input(), command.instructions());
            // 3. 高阶服务未暴露usage时保持未知，不伪造计费数据。
            return Result.success(
                    converter.response(text, modelName));
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
