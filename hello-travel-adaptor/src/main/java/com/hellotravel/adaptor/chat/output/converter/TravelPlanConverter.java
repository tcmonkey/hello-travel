package com.hellotravel.adaptor.chat.output.converter;

import com.hellotravel.model.travel.TravelPlanDraftDO;
import com.hellotravel.model.travel.TravelPlanGenerationDO;

import org.springframework.stereotype.Component;

/**
 * 将LangChain4j结构化草稿投影为稳定的规划生成结果。
 *
 * @author AIGenerator
 */
@Component
public final class TravelPlanConverter {

    private final ChatModelConverter chatModelConverter;

    public TravelPlanConverter(ChatModelConverter chatModelConverter) {
        this.chatModelConverter = chatModelConverter;
    }

    /**
     * 组合结构化草稿与模型调用证据。
     *
     * @param draft 结构化计划草稿
     * @param model 实际模型名
     * @return 稳定规划生成结果
     * @author AIGenerator
     */
    public TravelPlanGenerationDO generation(TravelPlanDraftDO draft, String model) {
        return new TravelPlanGenerationDO(
                draft, chatModelConverter.response("", model));
    }
}
