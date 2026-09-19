package com.hellotravel.model.travel;

import java.math.BigDecimal;
import java.util.List;

/**
 * 旅行对话的结构化意图，不包含任何模型框架类型。
 *
 * @param mode 对话模式
 * @param city 需要查询的城市
 * @param origin 出发地
 * @param destination 目的地
 * @param startDate 行程开始日期，ISO-8601
 * @param endDate 行程结束日期，ISO-8601
 * @param days 用户指定的游玩天数
 * @param travelers 出行人数
 * @param budget 用户表达的总预算
 * @param preferences 行程偏好
 * @param constraints 儿童、老人、无障碍或节奏等约束
 * @param weather 是否需要天气事实
 * @param route 是否需要路线事实
 * @param knowledge 是否需要知识库证据
 * @author AIGenerator
 */
public record TravelIntentDO(
        TravelIntentMode mode,
        String city,
        String origin,
        String destination,
        String startDate,
        String endDate,
        Integer days,
        Integer travelers,
        BigDecimal budget,
        List<String> preferences,
        List<String> constraints,
        boolean weather,
        boolean route,
        boolean knowledge) {

    /**
     * 将模型可选集合规范为不可变空集合。
     *
     * @author AIGenerator
     */
    public TravelIntentDO {
        preferences = preferences == null ? List.of() : List.copyOf(preferences);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
    }
}
