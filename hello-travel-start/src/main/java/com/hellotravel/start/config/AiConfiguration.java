package com.hellotravel.start.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.DisabledChatModel;
import dev.langchain4j.model.chat.DisabledStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.DisabledEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;

/**
 * 对话与知识嵌入模型的显式命名装配；未配置密钥时保留可启动的禁用模型。
 *
 * @author AIGenerator
 */
@Configuration
public class AiConfiguration {

    /**
     * 装配由旅行规划 AiService 显式引用的聊天模型。
     *
     * @param environment 运行环境配置
     * @return 已配置或禁用的聊天模型
     * @author AIGenerator
     */
    @Bean("travelChatModel")
    public ChatModel travelChatModel(Environment environment) {
        // 1. 未配置密钥时使用禁用模型，使离线装配不产生任何外部调用。
        String key = environment.getProperty("DASHSCOPE_API_KEY");
        if (key == null || key.isBlank()) {
            return new DisabledChatModel();
        }
        // 2. 以明确名称装配百炼兼容模型，由 AiService 统一复用。
        return OpenAiChatModel.builder()
                .apiKey(key)
                .baseUrl(environment.getProperty(
                        "DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"))
                .modelName(environment.getProperty("CHAT_MODEL", "qwen-plus"))
                .timeout(Duration.ofSeconds(45))
                .maxRetries(0)
                .maxCompletionTokens(
                        environment.getProperty(
                                "travel.model.output-reserve", Integer.class, 4096))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /**
     * 装配旅行回答AiService使用的流式聊天模型。
     *
     * @param environment 运行环境配置
     * @return 已配置或禁用的流式聊天模型
     * @author AIGenerator
     */
    @Bean("travelStreamingChatModel")
    public StreamingChatModel travelStreamingChatModel(Environment environment) {
        // 1. 未配置密钥时使用禁用模型，使离线装配不产生任何外部调用。
        String key = environment.getProperty("DASHSCOPE_API_KEY");
        if (key == null || key.isBlank()) {
            return new DisabledStreamingChatModel();
        }
        // 2. 以明确名称装配流式百炼兼容模型，由旅行回答AiService复用。
        return OpenAiStreamingChatModel.builder()
                .apiKey(key)
                .baseUrl(environment.getProperty(
                        "DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"))
                .modelName(environment.getProperty("CHAT_MODEL", "qwen-plus"))
                .timeout(Duration.ofSeconds(90))
                .maxCompletionTokens(
                        environment.getProperty(
                                "travel.model.output-reserve", Integer.class, 4096))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    /**
     * 装配知识库专用嵌入模型，维度与Milvus集合策略保持一致。
     *
     * @param environment 运行环境配置
     * @return 已配置或禁用的嵌入模型
     * @author AIGenerator
     */
    @Bean("knowledgeEmbeddingModel")
    public EmbeddingModel knowledgeEmbeddingModel(Environment environment) {
        // 1. 未配置密钥时使用禁用模型，使离线装配不产生外部调用。
        String key = environment.getProperty("DASHSCOPE_API_KEY");
        if (key == null || key.isBlank()) {
            return new DisabledEmbeddingModel();
        }
        // 2. 以知识索引策略的模型名和维度装配单例嵌入模型。
        return OpenAiEmbeddingModel.builder()
                .apiKey(key)
                .baseUrl(environment.getProperty(
                        "DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"))
                .modelName(KnowledgeIndexPolicy.model())
                .dimensions(KnowledgeIndexPolicy.dimensions())
                .timeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
