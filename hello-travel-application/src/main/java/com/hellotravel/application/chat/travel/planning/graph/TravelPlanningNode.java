package com.hellotravel.application.chat.travel.planning.graph;

/**
 * 旅行规划图和持久化检查点共用的稳定节点标识。
 *
 * @author AIGenerator
 */
public enum TravelPlanningNode {

    /**
     * 需求完整性校验。
     *
     * @author AIGenerator
     */
    VALIDATE_REQUEST("validate_request"),
    /**
     * 目的地资料证据收集。
     *
     * @author AIGenerator
     */
    COLLECT_DESTINATION_EVIDENCE("collect_destination_evidence"),
    /**
     * 实时天气和路线事实收集。
     *
     * @author AIGenerator
     */
    COLLECT_REALTIME_FACTS("collect_realtime_facts"),
    /**
     * 第一版结构化草稿生成。
     *
     * @author AIGenerator
     */
    GENERATE_DRAFT("generate_draft"),
    /**
     * 确定性领域草稿校验。
     *
     * @author AIGenerator
     */
    VALIDATE_DRAFT("validate_draft"),
    /**
     * 按违规项执行有界修订。
     *
     * @author AIGenerator
     */
    REVISE_DRAFT("revise_draft"),
    /**
     * 渲染通过校验的最终计划。
     *
     * @author AIGenerator
     */
    RENDER_PLAN("render_plan"),
    /**
     * 对缺失信息或未解决违规形成追问。
     *
     * @author AIGenerator
     */
    CLARIFY_REQUIREMENTS("clarify_requirements");

    /**
     * 稳定节点标识。
     *
     * @author AIGenerator
     */
    private final String key;

    /**
     * 建立稳定节点。
     *
     * @param key 节点标识
     * @author AIGenerator
     */
    TravelPlanningNode(String key) {
        this.key = key;
    }

    /**
     * 返回 LangGraph 和运行记录共同使用的节点标识。
     *
     * @return 稳定节点标识
     * @author AIGenerator
     */
    public String key() {
        return key;
    }
}
