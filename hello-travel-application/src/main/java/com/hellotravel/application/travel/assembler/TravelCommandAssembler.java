package com.hellotravel.application.travel.assembler;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.application.travel.command.TravelCommand;

import org.springframework.stereotype.Component;

/**
 * 意图到旅行工具端口的字段投影。
 *
 * @author AIGenerator
 */
@Component
public final class TravelCommandAssembler {

    /**
     * 将已解析旅行意图整体转换为只读工具输入。
     *
     * @param intent 本次转换的intent快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public TravelCommand fromIntent(JsonNode intent) {
        return new TravelCommand(
                intent.path("city").asText(),
                intent.path("origin").asText(),
                intent.path("destination").asText(),
                intent.path("weather").asBoolean());
    }
}
