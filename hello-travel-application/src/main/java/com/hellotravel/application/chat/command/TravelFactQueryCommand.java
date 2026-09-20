package com.hellotravel.application.chat.command;

/**
 * 只读旅行事实查询命令。
 *
 * @param city 天气查询城市
 * @param origin 路线起点
 * @param destination 路线终点
 * @param weather 是否查询天气
 * @param route 是否查询路线
 * @author AIGenerator
 */
public record TravelFactQueryCommand(
        String city, String origin, String destination, boolean weather, boolean route) {

    /**
     * 判断本轮是否确实需要调用外部实时工具。
     *
     * @return 天气或路线查询是否被请求
     * @author AIGenerator
     */
    public boolean requested() {
        return weather || route;
    }
}
