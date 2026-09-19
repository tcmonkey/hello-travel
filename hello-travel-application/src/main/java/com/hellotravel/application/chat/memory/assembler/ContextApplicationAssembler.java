package com.hellotravel.application.chat.memory.assembler;

import com.hellotravel.application.chat.context.policy.ChatContextPolicy;
import com.hellotravel.application.support.Json;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

import org.springframework.stereotype.Component;

/**
 * 上下文预算视图映射的唯一来源。
 *
 * @author AIGenerator
 */
@Component
public final class ContextApplicationAssembler {

    /**
     * 投影预算、压缩状态与服务商真实用量，不将未知用量伪装为零。
     *
     * @param budget 本次转换的budget快照
     * @param compression 本次转换的compression快照
     * @param actualInput 本次转换的actualInput快照
     * @param actualOutput 本次转换的actualOutput快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String snapshot(
            ContextBudgetValue budget,
            String compression,
            Integer actualInput,
            Integer actualOutput) {
        // 1. 整体投影已选模型预算和估算说明。
        var values =
                new java.util.LinkedHashMap<String, Object>(
                        java.util.Map.of(
                                "window",
                                budget.window(),
                                "inputEstimate",
                                budget.estimatedInput(),
                                "outputReserve",
                                budget.outputReserve(),
                                "safetyReserve",
                                budget.safetyReserve(),
                                "estimator",
                                "utf8-upper-v1",
                                "compression",
                                compression));
        // 2. 只有供应商提供的真实用量才进入实际使用字段。
        if (actualInput != null) {
            values.put("actualInputTokens", actualInput);
        }
        if (actualOutput != null) {
            values.put("actualOutputTokens", actualOutput);
        }
        // 3. 返回协议兼容的预算JSON投影。
        return Json.encode(values);
    }

    /**
     * 无任务时投影同一策略的初始预算。
     *
     * @param policy 本次转换的policy快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String initial(ChatContextPolicy policy) {
        return snapshot(policy.budget(0), "IDLE", null, null);
    }
}
