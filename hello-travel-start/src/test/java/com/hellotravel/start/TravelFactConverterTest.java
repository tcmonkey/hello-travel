package com.hellotravel.start;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hellotravel.adaptor.chat.output.converter.TravelFactConverter;
import com.hellotravel.util.JsonUtil;

import org.junit.jupiter.api.Test;

/**
 * 高德响应到模型可信事实的有界转换回归。
 *
 * @author AIGenerator
 */
class TravelFactConverterTest {

    @Test
    void weatherResponseIsReducedToBoundedHumanReadableFacts() {
        // 1. 构造包含必要天气字段和额外供应商字段的响应。
        var response = JsonUtil.read("""
                {"forecasts":[{"city":"景德镇","reporttime":"2026-09-19 20:00:00",
                "adcode":"360200","casts":[
                {"date":"2026-09-20","dayweather":"晴","daytemp":"30",
                "nightweather":"多云","nighttemp":"22","irrelevant":"drop"}]}]}
                """);

        // 2. 转换后只保留可理解且有时效的字段，不把原始JSON交给模型。
        String facts = new TravelFactConverter().weatherFacts(response);

        assertTrue(facts.contains("景德镇"));
        assertTrue(facts.contains("2026-09-20"));
        assertTrue(facts.contains("30℃"));
        assertFalse(facts.contains("irrelevant"));
        assertFalse(facts.contains("adcode"));
    }

    @Test
    void invalidRouteFactsAreRejectedInsteadOfRenderedAsZero() {
        // 1. 构造缺少有效距离和时长的服务商响应。
        var response = JsonUtil.read("""
                {"route":{"paths":[{"distance":"0","duration":"unknown"}]}}
                """);

        // 2. 转换器必须拒绝无效事实，交由OutAdaptor降级处理。
        assertThrows(
                IllegalArgumentException.class,
                () -> new TravelFactConverter().drivingFacts(response));
    }
}
