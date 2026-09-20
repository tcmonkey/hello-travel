package com.hellotravel.adaptor.knowledge.output.converter;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;
import com.hellotravel.model.knowledge.EmbeddingDO;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 将知识嵌入SDK结果转换为固定维度业务快照。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeEmbeddingConverter {

    /**
     * 核验并转换完整嵌入响应。
     *
     * @param response SDK嵌入响应
     * @return 受控嵌入结果
     * @author AIGenerator
     */
    public EmbeddingDO result(Response<List<Embedding>> response) {
        // 1. 转换并验证全部返回向量，异常向量不会进入Milvus。
        List<List<Float>> vectors = response.content().stream().map(this::vector).toList();
        // 2. 只记录供应商真实返回的输入用量。
        var usage = response.tokenUsage();
        return new EmbeddingDO(
                vectors,
                usage == null ? null : usage.inputTokenCount(),
                KnowledgeIndexPolicy.model());
    }

    private List<Float> vector(Embedding embedding) {
        // 1. 固定维度必须与知识集合schema一致。
        float[] raw = embedding.vector();
        if (raw.length != KnowledgeIndexPolicy.dimensions()) {
            throw new AdaptorException(AdaptorErrorCode.UNAVAILABLE);
        }
        // 2. 拒绝NaN与无穷值，避免污染向量集合。
        List<Float> values = new ArrayList<>(raw.length);
        for (float value : raw) {
            if (!Float.isFinite(value)) {
                throw new AdaptorException(AdaptorErrorCode.UNAVAILABLE);
            }
            values.add(value);
        }
        // 3. 交付不可变向量快照。
        return List.copyOf(values);
    }
}
