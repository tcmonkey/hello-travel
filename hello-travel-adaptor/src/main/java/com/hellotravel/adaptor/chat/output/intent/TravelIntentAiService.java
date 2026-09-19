package com.hellotravel.adaptor.chat.output.intent;

import com.hellotravel.model.travel.TravelIntentDO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * LangChain4j结构化旅行意图识别契约。
 *
 * @author AIGenerator
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "travelChatModel")
public interface TravelIntentAiService {

    /**
     * 识别三路旅行意图并抽取结构化字段。
     *
     * @param input 本轮用户输入
     * @param instructions 服务端可信规则
     * @return 结构化旅行意图
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}")
    TravelIntentDO recognize(@UserMessage String input, @V("instructions") String instructions);
}
