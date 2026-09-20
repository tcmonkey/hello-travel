package com.hellotravel.application.chat.command;

/**
 * 会话记忆模型命令。
 *
 * @param input 有界对话资料
 * @param instructions 服务端可信记忆规则
 * @author AIGenerator
 */
public record MemoryAgentCommand(String input, String instructions) {

    /**
     * 阻止诊断输出泄漏对话正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "MemoryAgentCommand{redacted}";
    }
}
