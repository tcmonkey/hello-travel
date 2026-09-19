package com.hellotravel.model.chat;

/**
 * 对话模型响应的内部快照。
 *
 * @param text 生成正文
 * @param inputTokens 供应商确实返回的输入用量
 * @param outputTokens 供应商确实返回的输出用量
 * @param providerId 供应商响应关联ID
 * @param model 实际配置模型
 * @author AIGenerator
 */
public record ChatModelDO(
        String text,
        Integer inputTokens,
        Integer outputTokens,
        String providerId,
        String model) {

    /**
     * 阻止诊断输出泄漏模型正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "ChatModelDO{redacted}";
    }
}
