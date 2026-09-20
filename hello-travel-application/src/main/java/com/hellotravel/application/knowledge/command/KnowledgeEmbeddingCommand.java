package com.hellotravel.application.knowledge.command;

/**
 * 知识文本嵌入命令。
 *
 * @param texts 有界文本批次
 * @author AIGenerator
 */
public record KnowledgeEmbeddingCommand(java.util.List<String> texts) {

    /**
     * 阻止诊断输出泄漏知识正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "KnowledgeEmbeddingCommand{redacted}";
    }
}
