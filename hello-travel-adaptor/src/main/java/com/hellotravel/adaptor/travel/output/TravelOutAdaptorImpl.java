package com.hellotravel.adaptor.travel.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.travel.adaptor.TravelOutAdaptor;
import com.hellotravel.application.travel.command.TravelCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelDO;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 固定高德只读端点；不接受模型生成的URL，失败明确降级而不编造事实。
 *
 * @author AIGenerator
 */
@Component
public final class TravelOutAdaptorImpl implements TravelOutAdaptor {

    private final Environment environment;

    public TravelOutAdaptorImpl(Environment environment) {
        this.environment = environment;
    }

    /**
     * 查询实时旅行事实并明确降级状态。
     *
     * @author AIGenerator
     * @param travelCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<TravelDO> consult(TravelCommand travelCommand) {
        try {
            String key = environment.getProperty("AMAP_MAPS_API_KEY");
            if (key == null || key.isBlank()) {
                return Result.success(new TravelDO("实时天气和路线工具未配置，请核实后出行。", false));
            }
            try {
                StringBuilder facts = new StringBuilder("高德查询时间：" + java.time.Instant.now() + "\n");
                if (travelCommand.weather() && valid(travelCommand.city())) {
                    var point = geo(key, travelCommand.city());
                    var weather =
                            get(
                                    "/v3/weather/weatherInfo",
                                    Map.of(
                                            "key",
                                            key,
                                            "city",
                                            point.path("adcode").asText(),
                                            "extensions",
                                            "all"));
                    facts.append("城市未来天气预报：").append(weather.path("forecasts")).append("\n");
                }
                if (valid(travelCommand.origin()) && valid(travelCommand.destination())) {
                    var origin = geo(key, travelCommand.origin());
                    var destination = geo(key, travelCommand.destination());
                    var route =
                            get(
                                    "/v3/direction/driving",
                                    Map.of(
                                            "key",
                                            key,
                                            "origin",
                                            origin.path("location").asText(),
                                            "destination",
                                            destination.path("location").asText(),
                                            "extensions",
                                            "base"));
                    var path = route.path("route").path("paths").path(0);
                    facts.append("驾车参考距离（米）：")
                            .append(path.path("distance").asText())
                            .append("；参考时长（秒）：")
                            .append(path.path("duration").asText())
                            .append("。非实时路况保证。\n");
                }
                if (facts.length() > 6000) {
                    return Result.success(new TravelDO("工具结果超限，请缩小查询范围。", false));
                }
                return Result.success(new TravelDO(facts.toString(), true));
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return Result.success(new TravelDO("实时工具暂不可用；天气、路程和耗时须用户另行核实。", false));
            }
        } catch (Exception exception) {
            return com.hellotravel.common.error.Failures.capture(
                    exception, com.hellotravel.adaptor.exception.AdaptorErrorCode.FAILED);
        }
    }

    private boolean valid(String value) {
        return value != null && !value.isBlank() && value.length() <= 120;
    }

    private JsonNode geo(String key, String address) throws Exception {
        var data = get("/v3/geocode/geo", Map.of("key", key, "address", address));
        var point = data.path("geocodes").path(0);
        if (!point.path("location").asText().matches("[0-9.]+,[0-9.]+")
                || !point.path("adcode").asText().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("location unavailable");
        }
        return point;
    }

    private JsonNode get(String path, Map<String, String> parameters) throws Exception {
        String query =
                parameters.entrySet().stream()
                        .map(
                                e ->
                                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                                                + "="
                                                + URLEncoder.encode(
                                                        e.getValue(), StandardCharsets.UTF_8))
                        .collect(java.util.stream.Collectors.joining("&"));
        var client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var request =
                HttpRequest.newBuilder(URI.create("https://restapi.amap.com" + path + "?" + query))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            byte[] bytes = stream.readNBytes(262145);
            if (response.statusCode() != 200 || bytes.length > 262144) {
                throw new IllegalStateException("tool unavailable");
            }
            var data = Json.read(new String(bytes, StandardCharsets.UTF_8));
            if (!"1".equals(data.path("status").asText())) {
                throw new IllegalStateException("tool unavailable");
            }
            return data;
        }
    }
}
