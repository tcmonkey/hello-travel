package com.hellotravel.application.chat.command;

import java.util.function.Predicate;

/**
 * 普通旅行对话模型命令。
 *
 * @param input 本轮用户输入
 * @param instructions 服务端可信回答规则
 * @param context 已核验的对话上下文
 * @param partial 流式全文进度回调
 * @author AIGenerator
 */
public record TravelDialogueCommand(
        String input, String instructions, String context, Predicate<String> partial) {

    /**
     * 阻止诊断输出泄漏用户正文和可信上下文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "TravelDialogueCommand{redacted}";
    }
}
