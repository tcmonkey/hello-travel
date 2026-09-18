package com.hellotravel.adaptor.model.output.converter;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.model.travel.ModelDO;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型能力端口与SDK之间的双向转换。
 *
 * @author AIGenerator
 */
@Component()
public final class ModelOutputConverter {

    /**
     * 专属知识集合固定向量维度。
     *
     * @author AIGenerator
     */
    public static final int VECTOR_DIMENSIONS = KnowledgeIndexPolicy.dimensions();

    /**
     * 专属嵌入模型。
     *
     * @author AIGenerator
     */
    public static final String EMBEDDING_MODEL = KnowledgeIndexPolicy.model();

    /**
     * 按端口角色映射LangChain消息，资料不允许提升为系统规则。
     *
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public List<ChatMessage> messages(ModelCommand command) {
        // 1. 首条系统规则只来自可信命令系统字段。
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(command.system()));
        // 2. 历史资料只能映射为用户或助手消息，拒绝其他角色。
        for (var item : command.messages()) {
            switch (item.role()) {
                case "ASSISTANT" -> messages.add(AiMessage.from(item.text()));
                case "USER" -> messages.add(UserMessage.from(item.text()));
                default -> throw new AdaptorException(AdaptorErrorCode.INVALID);
            }
        }
        // 3. 交付不可变SDK消息快照。
        return List.copyOf(messages);
    }

    /**
     * 转换完整聊天响应及服务商计费凭据。
     *
     * @param response 本次转换的response快照
     * @param model 本次转换的model快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ModelDO chat(ChatResponse response, String model) {
        // 1. 读取服务商实际返回用量，缺失时保持未知而不伪造为零。
        var usage = response.tokenUsage();
        // 2. 投影模型文本、调用标识及真实用量。
        return new ModelDO(
                response.aiMessage().text(),
                List.of(),
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount(),
                response.id(),
                model);
    }

    /**
     * 将批量嵌入输入转换为SDK文本片段。
     *
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public List<TextSegment> segments(ModelCommand command) {
        return command.texts().stream().map(TextSegment::from).toList();
    }

    /**
     * 转换完整嵌入响应，同时核验向量维度和有限数值。
     *
     * @param response 本次转换的response快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ModelDO embedding(Response<List<Embedding>> response) {
        // 1. 转换并验证全部返回向量，异常向量不会进入Milvus。
        var vectors = response.content().stream().map(this::vector).toList();
        // 2. 保留嵌入调用的真实用量及固定模型标识。
        var usage = response.tokenUsage();
        return new ModelDO(
                null,
                vectors,
                usage == null ? null : usage.inputTokenCount(),
                usage == null ? null : usage.outputTokenCount(),
                null,
                EMBEDDING_MODEL);
    }

    /**
     * 验证SDK向量后交付不可变浮点快照。
     *
     * @param embedding 本次转换的embedding快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public List<Float> vector(Embedding embedding) {
        // 1. 拒绝不匹配的维度，防止专属集合结构漂移。
        float[] raw = embedding.vector();
        if (raw.length != VECTOR_DIMENSIONS) {
            throw new AdaptorException(AdaptorErrorCode.UNAVAILABLE);
        }
        // 2. 逐项校验有限数值，拒绝NaN与无穷大。
        List<Float> values = new ArrayList<>(VECTOR_DIMENSIONS);
        for (float value : raw) {
            if (!Float.isFinite(value)) {
                throw new AdaptorException(AdaptorErrorCode.UNAVAILABLE);
            }
            values.add(value);
        }
        // 3. 返回不可变向量，防止后续修改影响请求。
        return List.copyOf(values);
    }
}
