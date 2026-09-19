package com.hellotravel.model.travel;

import com.hellotravel.model.chat.ChatModelDO;

/**
 * 旅行草稿与模型调用证据的稳定内部结果。
 *
 * @param draft 尚待领域校验的草稿
 * @param model 供应商确实返回的模型证据
 * @author AIGenerator
 */
public record TravelPlanGenerationDO(TravelPlanDraftDO draft, ChatModelDO model) {
}
