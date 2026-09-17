package com.hellotravel.application.model.command;

/**
 * 模型能力端口命令；回调仅内存使用，不写checkpoint。
 *
 * @param action INTENT/COMPRESSION/ANSWER/MEMORY_EXTRACTION/EMBED
 * @param system 受信任系统规则
 * @param messages 有界消息
 * @param texts 有界嵌入批次
 * @param partial 流式回调，false表示撤销
 * @author AIGenerator
 */
public record ModelCommand(
        String action,
        String system,
        java.util.List<PromptMessageCommand> messages,
        java.util.List<String> texts,
        java.util.function.Predicate<String> partial) {

    /**
     * 避免诊断输出泄漏密码、凭据、验证码或提示正文。
     *
     * @return 脱敏类型描述
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "ModelCommand{redacted}";
    }
}
