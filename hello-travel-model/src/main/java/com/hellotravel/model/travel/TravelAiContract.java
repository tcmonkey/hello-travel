package com.hellotravel.model.travel;

/**
 * 旅行应用与可替换 AI 实现共同识别的稳定契约版本。
 *
 * @author AIGenerator
 */
public final class TravelAiContract {

    /**
     * 当前旅行规划图结构版本。
     *
     * @author AIGenerator
     */
    public static final String GRAPH_REVISION = "travel-planning-v2";

    /**
     * 当前意图、对话和规划提示契约版本。
     *
     * @author AIGenerator
     */
    public static final String PROMPT_REVISION = "travel-ai-v2";

    /**
     * 工具类不允许创建实例。
     *
     * @author AIGenerator
     */
    private TravelAiContract() {
    }
}
