package com.hellotravel.application.chat.travel.planning.assembler;

import com.hellotravel.application.chat.travel.context.TravelContextService;
import com.hellotravel.application.chat.travel.context.TravelConversationContext;
import com.hellotravel.application.chat.travel.planning.command.TravelPlanGenerationCommand;
import com.hellotravel.application.chat.travel.planning.command.TravelPlanRevisionCommand;
import com.hellotravel.application.support.Json;
import com.hellotravel.domain.travel.model.param.TravelPlanDraftValidationParam;
import com.hellotravel.domain.travel.model.param.TravelPlanRequestValidationParam;
import com.hellotravel.model.travel.TravelPlanDayDO;
import com.hellotravel.model.travel.TravelPlanItemDO;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 集中组装规划模型命令，并将已通过规则校验的草稿渲染为用户回答。
 *
 * @author AIGenerator
 */
@Component
public final class TravelPlanAssembler {

    /**
     * 计划草稿生成规则。
     * @author AIGenerator
     */
    private static final String GENERATION_RULES = """
            生成结构化逐日旅行草稿。只能使用给定需求和证据；不得编造票价、库存、开放时间、路线时长或政策。
            每个活动包含HH:mm开始和结束时间、地点、活动、接驳方式与接驳分钟；相邻活动的时间间隔必须覆盖接驳分钟。
            证据已核实时verified=true并填写sourceId；无法核实时verified=false并填写verificationNote，同时加入unverifiedItems。
            安排用餐、休息和交通缓冲；优先级、提醒和待核实项必须具体。estimatedCost未知时为null，estimatedTotal不得小于已知明细合计。
            """;

    /**
     * 计划草稿修订规则。
     * @author AIGenerator
     */
    private static final String REVISION_RULES = """
            只修正领域校验列出的违规项，保持用户需求和已核验证据不变。不得通过删除必要行程、伪造事实或篡改预算绕过校验。
            """;

    private final TravelContextService travelContextService;

    public TravelPlanAssembler(TravelContextService travelContextService) {
        this.travelContextService = travelContextService;
    }

    /**
     * 组装计划草稿生成命令。
     *
     * @param context 当前旅行上下文
     * @return 草稿生成命令
     * @author AIGenerator
     */
    public TravelPlanGenerationCommand generation(TravelConversationContext context) {
        return new TravelPlanGenerationCommand(
                Json.encode(context.intent()),
                travelContextService.trustedContext(context),
                GENERATION_RULES);
    }

    /**
     * 组装计划草稿修订命令。
     *
     * @param context 当前旅行上下文
     * @return 草稿修订命令
     * @author AIGenerator
     */
    public TravelPlanRevisionCommand revision(TravelConversationContext context) {
        return new TravelPlanRevisionCommand(
                Json.encode(context.intent()),
                travelContextService.trustedContext(context),
                Json.encode(context.draft()),
                Json.encode(context.validation().violations()),
                REVISION_RULES);
    }

    /**
     * 组装旅行需求领域校验参数。
     *
     * @param context 当前旅行上下文
     * @return 需求校验参数
     * @author AIGenerator
     */
    public TravelPlanRequestValidationParam requestValidation(
            TravelConversationContext context) {
        return new TravelPlanRequestValidationParam(context.intent());
    }

    /**
     * 组装旅行草稿领域校验参数。
     *
     * @param context 当前旅行上下文
     * @return 草稿校验参数
     * @author AIGenerator
     */
    public TravelPlanDraftValidationParam draftValidation(
            TravelConversationContext context) {
        return new TravelPlanDraftValidationParam(context.intent(), context.draft());
    }

    /**
     * 渲染用户需要补充的信息。
     *
     * @param context 当前旅行上下文
     * @return 补充信息提示
     * @author AIGenerator
     */
    public String clarify(TravelConversationContext context) {
        // 1. 优先使用用户可直接补充的问题，没有问题时展示仍未修正的规则违规。
        String questions = context.validation().questions().isEmpty()
                ? String.join("\n", context.validation().violations())
                : String.join("\n", context.validation().questions());
        // 2. 返回确定性追问，不为缺失信息再次调用模型或自行猜测。
        return "为了给你生成可执行的旅行计划，还需要补充：\n" + questions;
    }

    /**
     * 渲染已经通过领域校验的旅行计划。
     *
     * @param context 当前旅行上下文
     * @return 可展示旅行计划
     * @author AIGenerator
     */
    public String render(TravelConversationContext context) {
        // 1. 渲染已通过领域规则的标题、摘要与逐日活动。
        StringBuilder text = new StringBuilder();
        text.append("# ").append(context.draft().title()).append("\n\n");
        text.append(context.draft().summary()).append("\n\n");
        for (TravelPlanDayDO day : context.draft().days()) {
            text.append("## ").append(day.date()).append(" · ").append(day.city()).append("\n");
            for (TravelPlanItemDO item : day.items()) {
                text.append("- ")
                        .append(item.startTime()).append('-').append(item.endTime())
                        .append("｜").append(item.place()).append("：").append(item.activity());
                if (item.transport() != null && !item.transport().isBlank()) {
                    text.append("（").append(item.transport());
                    if (item.transitMinutes() != null) {
                        text.append("，预留").append(item.transitMinutes()).append("分钟");
                    }
                    text.append('）');
                }
                if (!item.verified()) {
                    text.append("【待核实：").append(item.verificationNote()).append("】");
                } else if (item.sourceId() != null && !item.sourceId().isBlank()) {
                    text.append("【来源：").append(item.sourceId()).append("】");
                }
                if (item.estimatedCost() != null) {
                    text.append("【预估费用：").append(item.estimatedCost()).append("】");
                }
                text.append('\n');
            }
            text.append('\n');
        }
        // 2. 追加总费用、优先事项、温馨提示和仍需用户复核的信息。
        if (context.draft().estimatedTotal() != null) {
            text.append("## 费用概览\n- 当前预估总费用：")
                    .append(context.draft().estimatedTotal())
                    .append("\n\n");
        }
        appendList(text, "优先事项", context.draft().priorities());
        appendList(text, "温馨提示", context.draft().reminders());
        appendList(text, "出行前待核实", context.draft().unverifiedItems());
        return text.toString().strip();
    }

    private void appendList(StringBuilder text, String title, java.util.List<String> values) {
        // 1. 空集合不输出空标题，保持最终回答紧凑清晰。
        if (values.isEmpty()) {
            return;
        }
        // 2. 将同类事项按稳定Markdown列表渲染，避免依赖对象默认字符串。
        text.append("## ").append(title).append("\n");
        text.append(values.stream().map(value -> "- " + value).collect(Collectors.joining("\n")));
        text.append("\n\n");
    }
}
