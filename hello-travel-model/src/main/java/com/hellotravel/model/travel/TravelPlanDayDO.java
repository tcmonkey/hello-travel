package com.hellotravel.model.travel;

import java.util.List;

/**
 * 旅行计划中一个自然日的结构化草稿。
 *
 * @param date 当日日期，ISO-8601
 * @param city 当日主要城市
 * @param items 按时间排列的活动
 * @author AIGenerator
 */
public record TravelPlanDayDO(String date, String city, List<TravelPlanItemDO> items) {

    /**
     * 将模型可选活动规范为不可变空集合。
     *
     * @author AIGenerator
     */
    public TravelPlanDayDO {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
