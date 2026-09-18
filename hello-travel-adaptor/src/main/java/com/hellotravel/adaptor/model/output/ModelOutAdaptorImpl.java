package com.hellotravel.adaptor.model.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.model.output.converter.ModelOutputConverter;
import com.hellotravel.adaptor.model.output.model.ModelStreamHandler;
import com.hellotravel.application.model.adaptor.ModelOutAdaptor;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.application.model.policy.ModelContextPolicy;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.ModelDO;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 百炼OpenAI兼容接口，LangChain4j调用；关闭原始请求/响应日志和自动计费重试。
 *
 * @author AIGenerator
 */
@Component
public final class ModelOutAdaptorImpl implements ModelOutAdaptor {

    private final Environment environment;
    private final ModelContextPolicy contextPolicy;
    private final ModelOutputConverter modelOutputConverter;

    public ModelOutAdaptorImpl(
            Environment environment,
            ModelContextPolicy contextPolicy,
            ModelOutputConverter modelOutputConverter) {
        this.environment = environment;
        this.contextPolicy = contextPolicy;
        this.modelOutputConverter = modelOutputConverter;
    }

    /**
     * 调用有界模型接口，失败返回明确不可用分类。
     *
     * @author AIGenerator
     * @param modelCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<ModelDO> generate(ModelCommand modelCommand) {
        try {
            // 1. 准备当前操作的存储或签名标识。
            String key = environment.getProperty("DASHSCOPE_API_KEY");
            // 2. 缺少百炼凭据时明确返回未配置结果，不以模拟回答替代真实调用。
            if (key == null || key.isBlank()) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            // 3. 取得配置的第三方服务地址，供本段后续处理使用。
            String base =
                    environment.getProperty(
                            "DASHSCOPE_BASE_URL",
                            "https://dashscope.aliyuncs.com/compatible-mode/v1");
            String model = environment.getProperty("CHAT_MODEL", "qwen-plus");
            // 4. 按向量嵌入场景进入对应职责分支。
            if ("EMBED".equals(modelCommand.action())) {
                return embed(modelCommand, key, base);
            }
            // 5. 在创建SDK请求和计费IO前核验完整输入，再转换消息角色。
            if (!contextPolicy.accepts(modelCommand)) {
                return Result.failure(AdaptorErrorCode.CONTEXT_LIMIT);
            }
            List<ChatMessage> messages = modelOutputConverter.messages(modelCommand);
            // 6. 调用或准备助手输出，后续必须核对成功与归属。
            int output = contextPolicy.outputLimit(modelCommand.action());
            ChatResponse response;
            // 7. 按流式回答场景进入对应职责分支。
            if ("ANSWER".equals(modelCommand.action())) {
                var streaming =
                        OpenAiStreamingChatModel.builder()
                                .apiKey(key)
                                .baseUrl(base)
                                .modelName(model)
                                .timeout(Duration.ofSeconds(90))
                                .maxCompletionTokens(output)
                                .logRequests(false)
                                .logResponses(false)
                                .build();
                CompletableFuture<ChatResponse> complete = new CompletableFuture<>();
                AtomicReference<StreamingHandle> handle = new AtomicReference<>();
                streaming.chat(
                        messages, new ModelStreamHandler(modelCommand.partial(), complete, handle));
                try {
                    response = complete.get(95, TimeUnit.SECONDS);
                } finally {
                    if (!complete.isDone() && handle.get() != null) {
                        handle.get().cancel();
                    }
                }
            } else {
                var chat =
                        OpenAiChatModel.builder()
                                .apiKey(key)
                                .baseUrl(base)
                                .modelName(model)
                                .timeout(Duration.ofSeconds(45))
                                .maxRetries(0)
                                .maxCompletionTokens(output)
                                .logRequests(false)
                                .logResponses(false)
                                .build();
                response = chat.chat(messages);
            }
            // 8. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(modelOutputConverter.chat(response, model));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failure(AdaptorErrorCode.UNAVAILABLE);
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    private Result<ModelDO> embed(ModelCommand modelCommand, String key, String base) {
        // 1. 取得本次请求的嵌入结果，供本段后续处理使用。
        var embedding =
                OpenAiEmbeddingModel.builder()
                        .apiKey(key)
                        .baseUrl(base)
                        .modelName(ModelOutputConverter.EMBEDDING_MODEL)
                        .dimensions(ModelOutputConverter.VECTOR_DIMENSIONS)
                        .timeout(Duration.ofSeconds(30))
                        .maxRetries(0)
                        .logRequests(false)
                        .logResponses(false)
                        .build();
        // 2. 拒绝空嵌入输入和超过16项的批次，约束模型请求规模。
        if (modelCommand.texts() == null
                || modelCommand.texts().isEmpty()
                || modelCommand.texts().size() > 16) {
            return Result.failure(AdaptorErrorCode.INVALID);
        }
        // 3. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var response = embedding.embedAll(modelOutputConverter.segments(modelCommand));
        // 4. 将本层成功数据封装为标准结果，保持对外模型隔离。
        return Result.success(modelOutputConverter.embedding(response));
    }
}
