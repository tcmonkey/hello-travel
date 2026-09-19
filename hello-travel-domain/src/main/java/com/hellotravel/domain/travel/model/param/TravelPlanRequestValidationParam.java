package com.hellotravel.domain.travel.model.param;

import com.hellotravel.model.travel.TravelIntentDO;

/**
 * 旅行规划需求校验参数。
 *
 * @param intent 结构化旅行意图
 * @author AIGenerator
 */
public record TravelPlanRequestValidationParam(TravelIntentDO intent) {
}
