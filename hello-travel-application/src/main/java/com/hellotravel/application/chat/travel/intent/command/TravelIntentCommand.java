package com.hellotravel.application.chat.travel.intent.command;

/**
 * 旅行意图识别的受控输入。
 *
 * @param input 当前用户输入
 * @param instructions 服务端可信分类和抽取规则
 * @author AIGenerator
 */
public record TravelIntentCommand(String input, String instructions) {

    /**
     * 阻止诊断输出泄漏用户正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "TravelIntentCommand{redacted}";
    }
}
