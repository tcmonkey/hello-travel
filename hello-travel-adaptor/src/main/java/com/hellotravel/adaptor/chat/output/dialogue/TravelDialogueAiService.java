package com.hellotravel.adaptor.chat.output.dialogue;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import reactor.core.publisher.Flux;

/**
 * LangChain4j普通旅行对话契约。
 *
 * @author AIGenerator
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "travelChatModel",
        streamingChatModel = "travelStreamingChatModel")
public interface TravelDialogueAiService {

    /**
     * 流式生成普通旅行对话回答。
     *
     * @param input 本轮用户输入
     * @param instructions 服务端可信规则
     * @param context 已核验上下文
     * @return 流式回答片段
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}\n已核验上下文：{{context}}")
    Flux<String> answer(
            @UserMessage String input,
            @V("instructions") String instructions,
            @V("context") String context);
}
