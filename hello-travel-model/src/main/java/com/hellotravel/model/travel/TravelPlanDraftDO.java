package com.hellotravel.model.travel;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型生成且尚未经领域校验的旅行计划草稿。
 *
 * @param title 计划标题
 * @param summary 计划摘要
 * @param days 逐日行程
 * @param estimatedTotal 可得到时的预估总费用
 * @param priorities 建议优先确认的事项
 * @param reminders 出行提醒
 * @param unverifiedItems 必须由用户再次核实的信息
 * @author AIGenerator
 */
public record TravelPlanDraftDO(
        String title,
        String summary,
        List<TravelPlanDayDO> days,
        BigDecimal estimatedTotal,
        List<String> priorities,
        List<String> reminders,
        List<String> unverifiedItems) {

    /**
     * 将模型可选集合规范为不可变空集合。
     *
     * @author AIGenerator
     */
    public TravelPlanDraftDO {
        days = days == null ? List.of() : List.copyOf(days);
        priorities = priorities == null ? List.of() : List.copyOf(priorities);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        unverifiedItems = unverifiedItems == null ? List.of() : List.copyOf(unverifiedItems);
    }
}
