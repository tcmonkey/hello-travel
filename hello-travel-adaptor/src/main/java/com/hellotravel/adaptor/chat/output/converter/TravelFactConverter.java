package com.hellotravel.adaptor.chat.output.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.model.travel.TravelDO;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * 高德请求字段、响应单位及明确降级结果的双向转换。
 *
 * @author AIGenerator
 */
@Component
public final class TravelFactConverter {

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
        // 1. 只读取第一组城市预报和最多七天结构化字段，拒绝把原始供应商JSON送入模型。
        JsonNode forecast = response.path("forecasts").path(0);
        JsonNode casts = forecast.path("casts");
        if (forecast.isMissingNode() || !casts.isArray() || casts.isEmpty()) {
            throw new IllegalArgumentException("weather forecast unavailable");
        }
        StringBuilder facts = new StringBuilder();
        facts.append("天气预报城市：")
                .append(required(forecast, "city"))
                .append("；发布时间：")
                .append(required(forecast, "reporttime"))
                .append('\n');
        // 2. 显式写出日期、昼夜天气和温度，省略供应商无关字段并控制上下文长度。
        int limit = Math.min(7, casts.size());
        for (int index = 0; index < limit; index++) {
            JsonNode cast = casts.path(index);
            facts.append(required(cast, "date"))
                    .append("：白天")
                    .append(required(cast, "dayweather"))
                    .append(' ')
                    .append(required(cast, "daytemp"))
                    .append("℃，夜间")
                    .append(required(cast, "nightweather"))
                    .append(' ')
                    .append(required(cast, "nighttemp"))
                    .append("℃。\n");
        }
        return facts.toString();
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
        long distance = positiveLong(path, "distance");
        long duration = positiveLong(path, "duration");
        // 2. 显式附带单位和非实时路况保证说明。
        return "驾车参考距离："
                + String.format(Locale.ROOT, "%.1f", distance / 1000.0)
                + "公里；参考时长："
                + Math.max(1, Math.round(duration / 60.0))
                + "分钟"
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

    private String required(JsonNode node, String field) {
        // 1. 读取并规范供应商必要字段，空白值不能进入模型上下文。
        String value = node.path(field).asText("").strip();
        // 2. 缺失字段时拒绝形成看似完整的实时事实，否则返回规范值。
        if (value.isEmpty()) {
            throw new IllegalArgumentException("required fact missing");
        }
        return value;
    }

    private long positiveLong(JsonNode node, String field) {
        // 1. 取得已通过非空校验的供应商数值文本。
        String value = required(node, field);
        // 2. 将距离或时长转换为正整数，非法格式统一拒绝。
        try {
            // 1. 解析十进制整数，不接受隐式小数或附带单位的文本。
            long parsed = Long.parseLong(value);
            // 2. 零值或负数不能作为路线依据，正数才能返回调用方。
            if (parsed <= 0) {
                throw new IllegalArgumentException("route fact invalid");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("route fact invalid", exception);
        }
    }
}
