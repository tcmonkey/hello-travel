package com.hellotravel.application.chat.travel.assembler;

import com.fasterxml.jackson.databind.JsonNode;
import com.hellotravel.application.chat.travel.command.TravelGenerateCommand;
import com.hellotravel.application.chat.travel.result.TravelGenerateResult;

import org.springframework.stereotype.Component;

/**
 * 旅行总用例输入与输出模型的集中装配器。
 *
 * @author AIGenerator
 */
@Component
public final class TravelApplicationAssembler {

    /**
     * 从已校验的Outbox载荷组装旅行生成命令。
     *
     * @param payload 发件箱结构化载荷
     * @return 旅行生成命令
     * @author AIGenerator
     */
    public TravelGenerateCommand generate(JsonNode payload) {
        return new TravelGenerateCommand(payload.path("runId").asText());
    }

    /**
     * 组装已形成明确终态的生成结果。
     *
     * @return 旅行生成结果
     * @author AIGenerator
     */
    public TravelGenerateResult completed() {
        return new TravelGenerateResult(true);
    }
}
