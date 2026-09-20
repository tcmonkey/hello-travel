package com.hellotravel.application.knowledge.assembler;

import com.hellotravel.application.knowledge.command.KnowledgeEmbeddingCommand;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组装知识文本嵌入命令。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeEmbeddingAppAssembler {

    /**
     * 冻结本次有界文本批次。
     *
     * @param texts 待嵌入文本
     * @return 不可变嵌入命令
     * @author AIGenerator
     */
    public KnowledgeEmbeddingCommand command(List<String> texts) {
        return new KnowledgeEmbeddingCommand(List.copyOf(texts));
    }
}
