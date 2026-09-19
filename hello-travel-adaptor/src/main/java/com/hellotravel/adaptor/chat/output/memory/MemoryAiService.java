package com.hellotravel.adaptor.chat.output.memory;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 会话摘要与显式记忆提取的高阶 LangChain4j 契约。
 *
 * @author AIGenerator
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "travelChatModel")
public interface MemoryAiService {

    /**
     * 压缩有界对话资料。
     *
     * @param input 对话资料
     * @param instructions 服务端可信摘要规则
     * @return 摘要文本
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}")
    String summarize(@UserMessage String input, @V("instructions") String instructions);

    /**
     * 提取用户明确要求保存的事实。
     *
     * @param input 原始用户消息
     * @param instructions 服务端可信提取规则
     * @return 事实JSON文本
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}")
    String extract(@UserMessage String input, @V("instructions") String instructions);
}
