package com.hellotravel.model.travel;

/**
 * 外部事实及降级状态。
 *
 * @param facts 有时间标记的有界事实
 * @param available 工具是否成功
 * @author AIGenerator
 */
public record TravelDO(String facts, boolean available) {
}
