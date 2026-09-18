package com.hellotravel.adaptor.model.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.model.output.model.ModelStreamHandler;
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
            // 5. 取得本次模型调用的消息列表，供本段后续处理使用。
            List<ChatMessage> messages = new ArrayList<>();
            // 6. 执行add职责步骤，并把失败交给所属事务或入口处理。
            messages.add(SystemMessage.from(modelCommand.system()));
            // 7. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
            for (var item : modelCommand.messages()) {
                messages.add(
                        "ASSISTANT".equals(item.role())
                                ? AiMessage.from(item.text())
                                : UserMessage.from(item.text()));
            }
            // 8. 调用或准备助手输出，后续必须核对成功与归属。
            int output = "ANSWER".equals(modelCommand.action()) ? 4096 : 2048;
            ChatResponse response;
            // 9. 按流式回答场景进入对应职责分支。
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
            // 10. 取得服务商实际返回的token使用量，供本段后续处理使用。
            var usage = response.tokenUsage();
            // 11. 将本层成功数据封装为标准结果，保持对外模型隔离。
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

    private Result<ModelDO> embed(ModelCommand modelCommand, String key, String base) {
        // 1. 取得本次请求的嵌入结果，供本段后续处理使用。
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
        // 2. 拒绝空嵌入输入和超过16项的批次，约束模型请求规模。
        if (modelCommand.texts() == null
                || modelCommand.texts().isEmpty()
                || modelCommand.texts().size() > 16) {
            return Result.failure(AdaptorErrorCode.INVALID);
        }
        // 3. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var response =
                embedding.embedAll(modelCommand.texts().stream().map(TextSegment::from).toList());
        List<List<Float>> vectors =
                response.content().stream()
                        .map(
                                x -> {
                                    // 1. 取得协议返回的原始内容，供本段后续处理使用。
                                    float[] raw = x.vector();
                                    // 2. 校验向量维度及数值有限性，拒绝异常嵌入结果。
                                    if (raw.length != 1024) {
                                        throw new IllegalStateException("embedding dimension");
                                    }
                                    // 3. 取得待序列化的上下文用量字段，供本段后续处理使用。
                                    List<Float> values = new ArrayList<>(1024);
                                    // 4. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                                    for (float v : raw) {
                                        if (!Float.isFinite(v)) {
                                            throw new IllegalStateException("embedding value");
                                        }
                                        values.add(v);
                                    }
                                    // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                                    return List.copyOf(values);
                                })
                        .toList();
        var usage = response.tokenUsage();
        // 4. 将本层成功数据封装为标准结果，保持对外模型隔离。
        return Result.success(
                new ModelDO(
                        null,
                        vectors,
                        usage == null ? null : usage.inputTokenCount(),
                        usage == null ? null : usage.outputTokenCount(),
                        null,
                        "text-embedding-v4"));
    }
}
