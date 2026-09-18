package com.hellotravel.adaptor.travel.output.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.model.travel.TravelDO;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 高德请求字段、响应单位及明确降级结果的双向转换。
 *
 * @author AIGenerator
 */
@Component
public final class TravelOutputConverter {

    /**
     * 明确展示notConfigured实时事实状态。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public TravelDO notConfigured() {
        return new TravelDO("实时天气和路线工具未配置，请核实后出行。", false);
    }

    /**
     * 明确展示exceeded实时事实状态。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public TravelDO exceeded() {
        return new TravelDO("工具结果超限，请缩小查询范围。", false);
    }

    /**
     * 明确展示unavailable实时事实状态。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public TravelDO unavailable() {
        return new TravelDO("实时工具暂不可用；天气、路程和耗时须用户另行核实。", false);
    }

    /**
     * 转换已查询旅行事实及可用标记。
     *
     * @param facts 本次转换的facts快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public TravelDO available(StringBuilder facts) {
        return new TravelDO(facts.toString(), true);
    }

    /**
     * 绑定地理编码请求，不接受模型生成的URL。
     *
     * @param key 本次转换的key快照
     * @param address 本次转换的address快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public Map<String, String> geocode(String key, String address) {
        return Map.of("key", key, "address", address);
    }

    /**
     * 绑定已核验行政区的天气请求。
     *
     * @param key 本次转换的key快照
     * @param point 本次转换的point快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public Map<String, String> weather(String key, JsonNode point) {
        return Map.of("key", key, "city", point.path("adcode").asText(), "extensions", "all");
    }

    /**
     * 绑定已核验坐标的驾车请求。
     *
     * @param key 本次转换的key快照
     * @param origin 本次转换的origin快照
     * @param destination 本次转换的destination快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public Map<String, String> driving(String key, JsonNode origin, JsonNode destination) {
        return Map.of(
                "key",
                key,
                "origin",
                origin.path("location").asText(),
                "destination",
                destination.path("location").asText(),
                "extensions",
                "base");
    }

    /**
     * 将有界SDK响应投影为带单位的天气说明。
     *
     * @param response 本次转换的response快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String weatherFacts(JsonNode response) {
        return "城市未来天气预报：" + response.path("forecasts") + "\n";
    }

    /**
     * 将驾车SDK响应投影为带单位及准确性说明的事实。
     *
     * @param response 本次转换的response快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String drivingFacts(JsonNode response) {
        // 1. 只读取服务商第一条参考路线，不猜测缺失字段。
        var path = response.path("route").path("paths").path(0);
        // 2. 显式附带单位和非实时路况保证说明。
        return "驾车参考距离（米）："
                + path.path("distance").asText()
                + "；参考时长（秒）："
                + path.path("duration").asText()
                + "。非实时路况保证。\n";
    }

    /**
     * 编码参数并生成固定高德域名的只读HTTP请求。
     *
     * @param path 本次转换的path快照
     * @param parameters 本次转换的parameters快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public HttpRequest request(String path, Map<String, String> parameters) {
        // 1. 对每个参数分别编码，防止地点名称改变查询结构。
        String query =
                parameters.entrySet().stream()
                        .map(
                                e ->
                                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                                + "="
                                                + URLEncoder.encode(
                                                        e.getValue(), StandardCharsets.UTF_8))
                        .collect(java.util.stream.Collectors.joining("&"));
        // 2. 固定服务商域名与超时，不接受外部URL或无限等待。
        return HttpRequest.newBuilder(URI.create("https://restapi.amap.com" + path + "?" + query))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
    }
}
