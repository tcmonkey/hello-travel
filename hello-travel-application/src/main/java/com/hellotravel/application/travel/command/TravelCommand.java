package com.hellotravel.application.travel.command;

/**
 * 只读旅行事实工具请求。
 *
 * @param city 目标城市
 * @param origin 路线起点
 * @param destination 路线终点
 * @param weather 是否查询预报
 * @author AIGenerator
 */
public record TravelCommand(String city, String origin, String destination, boolean weather) {
}
