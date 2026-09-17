package com.hellotravel.adaptor.model.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.model.adaptor.ModelOutAdaptor;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.ModelDO;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingHandle;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
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

    public ModelOutAdaptorImpl(Environment environment) {
        this.environment = environment;
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
            String key = environment.getProperty("DASHSCOPE_API_KEY");
            if (key == null || key.isBlank()) {
                return Result.failure(AdaptorErrorCode.UNAVAILABLE);
            }
            String base =
                    environment.getProperty(
                            "DASHSCOPE_BASE_URL",
                            "https://dashscope.aliyuncs.com/compatible-mode/v1");
            String model = environment.getProperty("CHAT_MODEL", "qwen-plus");
            if ("EMBED".equals(modelCommand.action())) {
                var embedding =
                        OpenAiEmbeddingModel.builder()
                                .apiKey(key)
                                .baseUrl(base)
                                .modelName("text-embedding-v4")
                                .dimensions(1024)
                                .timeout(Duration.ofSeconds(30))
                                .maxRetries(0)
                                .logRequests(false)
                                .logResponses(false)
                                .build();
                if (modelCommand.texts() == null
                        || modelCommand.texts().isEmpty()
                        || modelCommand.texts().size() > 16) {
                    return Result.failure(AdaptorErrorCode.INVALID);
                }
                var response =
                        embedding.embedAll(
                                modelCommand.texts().stream().map(TextSegment::from).toList());
                List<List<Float>> vectors =
                        response.content().stream()
                                .map(
                                        x -> {
                                            float[] raw = x.vector();
                                            if (raw.length != 1024) {
                                                throw new IllegalStateException(
                                                        "embedding dimension");
                                            }
                                            List<Float> values = new ArrayList<>(1024);
                                            for (float v : raw) {
                                                if (!Float.isFinite(v)) {
                                                    throw new IllegalStateException(
                                                            "embedding value");
                                                }
                                                values.add(v);
                                            }
                                            return List.copyOf(values);
                                        })
                                .toList();
                var usage = response.tokenUsage();
                return Result.success(
                        new ModelDO(
                                null,
                                vectors,
                                usage == null ? null : usage.inputTokenCount(),
                                usage == null ? null : usage.outputTokenCount(),
                                null,
                                "text-embedding-v4"));
            }
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(modelCommand.system()));
            for (var item : modelCommand.messages()) {
                messages.add(
                        "ASSISTANT".equals(item.role())
                                ? AiMessage.from(item.text())
                                : UserMessage.from(item.text()));
            }
            int output = "ANSWER".equals(modelCommand.action()) ? 4096 : 2048;
            ChatResponse response;
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
                        messages,
                        new com.hellotravel.adaptor.model.output.model.ModelStreamHandler(
                                modelCommand.partial(), complete, handle));
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
            var usage = response.tokenUsage();
            return Result.success(
                    new ModelDO(
                            response.aiMessage().text(),
                            List.of(),
                            usage == null ? null : usage.inputTokenCount(),
                            usage == null ? null : usage.outputTokenCount(),
                            response.id(),
                            model));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failure(AdaptorErrorCode.UNAVAILABLE);
        } catch (Exception exception) {
            return Result.failure(AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
