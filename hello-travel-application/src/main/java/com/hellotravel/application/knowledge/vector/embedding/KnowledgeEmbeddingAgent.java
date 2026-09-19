package com.hellotravel.application.knowledge.vector.embedding;

import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.EmbeddingDO;

/**
 * 知识文本嵌入端口。
 *
 * @author AIGenerator
 */
public interface KnowledgeEmbeddingAgent {

    /**
     * 将有界知识文本批次转换为固定维度向量。
     *
     * @param knowledgeEmbeddingCommand 已核验的嵌入命令
     * @return 嵌入结果
     * @author AIGenerator
     */
    Result<EmbeddingDO> embed(KnowledgeEmbeddingCommand knowledgeEmbeddingCommand);
}
