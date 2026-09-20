package com.hellotravel.adaptor.chat.output.aiservice;

import com.hellotravel.model.travel.TravelPlanDraftDO;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * LangChain4j结构化旅行计划草稿契约。
 *
 * @author AIGenerator
 */
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "travelChatModel")
public interface TravelPlanAiService {

    /**
     * 生成第一版结构化计划草稿。
     *
     * @param requirements 结构化旅行需求
     * @param evidence 已核验证据
     * @param instructions 服务端可信规则
     * @return 结构化计划草稿
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}\n已核验证据：{{evidence}}")
    TravelPlanDraftDO generate(
            @UserMessage String requirements,
            @V("evidence") String evidence,
            @V("instructions") String instructions);

    /**
     * 根据确定性违规项修订计划草稿。
     *
     * @param requirements 结构化旅行需求
     * @param evidence 已核验证据
     * @param draft 当前计划草稿
     * @param violations 领域违规项
     * @param instructions 服务端可信规则
     * @return 修订后的结构化草稿
     * @author AIGenerator
     */
    @SystemMessage("{{instructions}}\n已核验证据：{{evidence}}\n当前草稿：{{draft}}\n违规项：{{violations}}")
    TravelPlanDraftDO revise(
            @UserMessage String requirements,
            @V("evidence") String evidence,
            @V("draft") String draft,
            @V("violations") String violations,
            @V("instructions") String instructions);
}
