package com.hellotravel.application.knowledge.assembler;

import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.application.knowledge.command.VectorItemCommand;
import com.hellotravel.domain.knowledge.model.entity.IndexJobEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeChunkEntity;
import com.hellotravel.domain.knowledge.model.entity.KnowledgeDocumentEntity;

import org.springframework.stereotype.Component;

/**
 * 持久知识快照到向量能力请求的整体映射。
 *
 * @author AIGenerator
 */
@Component()
public final class VectorCommandAssembler {

    /**
     * 绑定当前用户检索向量，调用方不能设置无关索引字段。
     *
     * @param owner 本次转换的owner快照
     * @param query 本次转换的query快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorCommand search(String owner, java.util.List<Float> query) {
        return new VectorCommand(
                "SEARCH", owner, null, 0L, java.util.List.of(), java.util.List.copyOf(query));
    }

    /**
     * 删除指定归属文档的所有向量。
     *
     * @param owner 本次转换的owner快照
     * @param document 本次转换的document快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorCommand delete(String owner, KnowledgeDocumentEntity document) {
        return new VectorCommand(
                "DELETE",
                owner,
                document.publicId(),
                document.indexGeneration(),
                java.util.List.of(),
                null);
    }

    /**
     * 按当前有效代次清理过期向量，保留明确租约代次。
     *
     * @param owner 本次转换的owner快照
     * @param document 本次转换的document快照
     * @param generation 本次转换的generation快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorCommand reconcile(
            String owner, KnowledgeDocumentEntity document, Long generation) {
        return new VectorCommand(
                "RECONCILE", owner, document.publicId(), generation, java.util.List.of(), null);
    }

    /**
     * 将当前任务、文档与批次整体投影为upsert请求。
     *
     * @param owner 本次转换的owner快照
     * @param document 本次转换的document快照
     * @param job 本次转换的job快照
     * @param items 本次转换的items快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorCommand upsert(
            String owner,
            KnowledgeDocumentEntity document,
            IndexJobEntity job,
            java.util.List<VectorItemCommand> items) {
        return new VectorCommand(
                "UPSERT",
                owner,
                document.publicId(),
                job.indexGeneration(),
                java.util.List.copyOf(items),
                null);
    }

    /**
     * 从持久分块及嵌入结果构造向量项，摘要来自持久快照。
     *
     * @param chunk 本次转换的chunk快照
     * @param document 本次转换的document快照
     * @param vector 本次转换的vector快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorItemCommand item(
            KnowledgeChunkEntity chunk,
            KnowledgeDocumentEntity document,
            java.util.List<Float> vector) {
        return new VectorItemCommand(
                chunk.vectorKey(),
                document.publicId(),
                chunk.indexGeneration(),
                chunk.chunkNo(),
                java.util.HexFormat.of().formatHex(chunk.contentSha256()),
                java.util.List.copyOf(vector));
    }
}
