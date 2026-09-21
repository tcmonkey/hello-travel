package com.hellotravel.adaptor.knowledge.output;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.knowledge.output.converter.KnowledgeEmbeddingConverter;
import com.hellotravel.application.knowledge.adaptor.KnowledgeEmbeddingAgent;
import com.hellotravel.application.knowledge.command.KnowledgeEmbeddingCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.EmbeddingDO;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 知识文本嵌入端口的LangChain4j实现。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeEmbeddingAgentImpl implements KnowledgeEmbeddingAgent {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeEmbeddingConverter converter;

    public KnowledgeEmbeddingAgentImpl(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            KnowledgeEmbeddingConverter converter) {
        this.embeddingModel = embeddingModel;
        this.converter = converter;
    }

    /**
     * 核验批次边界后复用start层命名嵌入模型。
     *
     * @param knowledgeEmbeddingCommand 已核验的嵌入命令
     * @return 嵌入结果
     * @author AIGenerator
     */
    @Override
    public Result<EmbeddingDO> embed(KnowledgeEmbeddingCommand knowledgeEmbeddingCommand) {
        try {
            // 1. 拒绝空批次及超过知识索引任务上限的请求。
            if (knowledgeEmbeddingCommand.texts() == null
                    || knowledgeEmbeddingCommand.texts().isEmpty()
                    || knowledgeEmbeddingCommand.texts().size() > 16) {
                return Result.failure(AdaptorErrorCode.INVALID);
            }
            // 2. 转换为LangChain4j文本片段并调用复用的命名模型。
            var response =
                    embeddingModel.embedAll(
                            knowledgeEmbeddingCommand.texts().stream()
                                    .map(TextSegment::from)
                                    .toList());
            // 3. 核验并转换固定维度向量结果。
            return Result.success(converter.result(response));
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }
}
