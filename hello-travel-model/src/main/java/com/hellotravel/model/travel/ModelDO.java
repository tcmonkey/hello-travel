package com.hellotravel.model.travel;

/**
 * 供应商响应转换后的内部结果。
 *
 * @param text 生成正文
 * @param vectors 1024维嵌入批次
 * @param inputTokens 供应商确实返回的输入用量
 * @param outputTokens 供应商确实返回的输出用量
 * @param providerId 供应商响应关联ID
 * @param model 实际配置模型
 * @author AIGenerator
 */
public record ModelDO(
        String text,
        java.util.List<java.util.List<Float>> vectors,
        Integer inputTokens,
        Integer outputTokens,
        String providerId,
        String model) {

    /**
     * 阻止诊断输出泄漏安全值或模型正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "ModelDO{redacted}";
    }
}
