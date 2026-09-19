package com.hellotravel.model.knowledge;

/**
 * 知识文本嵌入后的内部快照。
 *
 * @param vectors 有界向量批次
 * @param inputTokens 供应商确实返回的输入用量
 * @param model 实际嵌入模型
 * @author AIGenerator
 */
public record EmbeddingDO(
        java.util.List<java.util.List<Float>> vectors,
        Integer inputTokens,
        String model) {

    /**
     * 阻止诊断输出泄漏向量数据。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "EmbeddingDO{redacted}";
    }
}
