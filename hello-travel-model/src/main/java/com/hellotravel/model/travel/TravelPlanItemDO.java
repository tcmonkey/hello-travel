package com.hellotravel.model.travel;

import java.math.BigDecimal;

/**
 * 旅行计划中一个可排程活动。
 *
 * @param startTime 开始时间，HH:mm
 * @param endTime 结束时间，HH:mm
 * @param place 地点名称
 * @param activity 活动内容
 * @param transport 与前一项的接驳方式
 * @param transitMinutes 预留接驳分钟数
 * @param estimatedCost 已知或预估费用
 * @param sourceId 支撑开放、政策等事实的来源标识
 * @param verified 时效性事实是否已核验
 * @param verificationNote 未核实原因或复核提示
 * @author AIGenerator
 */
public record TravelPlanItemDO(
        String startTime,
        String endTime,
        String place,
        String activity,
        String transport,
        Integer transitMinutes,
        BigDecimal estimatedCost,
        String sourceId,
        boolean verified,
        String verificationNote) {
}
