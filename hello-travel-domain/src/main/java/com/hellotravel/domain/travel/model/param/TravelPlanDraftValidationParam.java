package com.hellotravel.domain.travel.model.param;

import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelPlanDraftDO;

/**
 * 旅行计划草稿校验参数。
 *
 * @param intent 已核对的旅行意图
 * @param draft 模型生成的结构化草稿
 * @author AIGenerator
 */
public record TravelPlanDraftValidationParam(
        TravelIntentDO intent, TravelPlanDraftDO draft) {
}
