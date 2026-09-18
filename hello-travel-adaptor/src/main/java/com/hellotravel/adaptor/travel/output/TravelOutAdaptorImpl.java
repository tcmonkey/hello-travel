package com.hellotravel.adaptor.travel.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.travel.output.converter.TravelOutputConverter;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.travel.adaptor.TravelOutAdaptor;
import com.hellotravel.application.travel.command.TravelCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.travel.TravelDO;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
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
    private final TravelOutputConverter travelOutputConverter;

    public TravelOutAdaptorImpl(
            Environment environment, TravelOutputConverter travelOutputConverter) {
        this.environment = environment;
        this.travelOutputConverter = travelOutputConverter;
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
            // 1. 准备当前操作的存储或签名标识。
            String key = environment.getProperty("AMAP_MAPS_API_KEY");
            // 2. 缺少高德凭据时返回未配置结果，由用例向用户说明实时事实不可用。
            if (key == null || key.isBlank()) {
                return Result.success(travelOutputConverter.notConfigured());
            }
            // 3. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
            try {
                // 1. 取得已验证的长期事实候选列表，供本段后续处理使用。
                StringBuilder facts = new StringBuilder("高德查询时间：" + java.time.Instant.now() + "\n");
                // 2. 仅对有效城市请求天气，缺失目的地时不猜测实时数据。
                if (travelCommand.weather() && valid(travelCommand.city())) {
                    var point = geo(key, travelCommand.city());
                    var weather =
                            get(
                                    "/v3/weather/weatherInfo",
                                    travelOutputConverter.weather(key, point));
                    facts.append(travelOutputConverter.weatherFacts(weather));
                }
                // 3. 仅在起终点都有效时查询路程，避免不完整路线请求。
                if (valid(travelCommand.origin()) && valid(travelCommand.destination())) {
                    var origin = geo(key, travelCommand.origin());
                    var destination = geo(key, travelCommand.destination());
                    var route =
                            get(
                                    "/v3/direction/driving",
                                    travelOutputConverter.driving(key, origin, destination));
                    facts.append(travelOutputConverter.drivingFacts(route));
                }
                // 4. 依据格式、长度或数量边界处理分支，避免继续使用无效数据。
                if (facts.length() > 6000) {
                    return Result.success(travelOutputConverter.exceeded());
                }
                // 5. 将本层成功数据封装为标准结果，保持对外模型隔离。
                return Result.success(travelOutputConverter.available(facts));
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return Result.success(travelOutputConverter.unavailable());
            }
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.FAILED);
        }
    }

    private boolean valid(String value) {
        return value != null && !value.isBlank() && value.length() <= 120;
    }

    private JsonNode geo(String key, String address) throws Exception {
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        var data = get("/v3/geocode/geo", travelOutputConverter.geocode(key, address));
        var point = data.path("geocodes").path(0);
        // 2. 依据格式、长度或数量边界处理分支，避免继续使用无效数据。
        if (!point.path("location").asText().matches("[0-9.]+,[0-9.]+")
                || !point.path("adcode").asText().matches("[0-9]{6}")) {
            throw new IllegalArgumentException("location unavailable");
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return point;
    }

    private JsonNode get(String path, Map<String, String> parameters) throws Exception {
        // 1. 建立禁止重定向且有连接超时的客户端，转换固定端点请求后执行。
        var client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        var request = travelOutputConverter.request(path, parameters);
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
        try (var stream = response.body()) {
            // 1. 取得待校验的上传文件字节，供本段后续处理使用。
            byte[] bytes = stream.readNBytes(262145);
            // 2. 核对第三方HTTP状态，非成功响应不得解析成正常业务数据。
            if (response.statusCode() != 200 || bytes.length > 262144) {
                throw new IllegalStateException("tool unavailable");
            }
            // 3. 取得本段结果并准备本层转换，随后显式核对成功状态。
            var data = Json.read(new String(bytes, StandardCharsets.UTF_8));
            // 4. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
            if (!"1".equals(data.path("status").asText())) {
                throw new IllegalStateException("tool unavailable");
            }
            // 5. 返回本段实际处理结果，保持本层输出契约。
            return data;
        }
    }
}
