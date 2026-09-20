package com.hellotravel.application.chat.travel;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 规划图状态适配器；只保存可序列化执行键，业务对象由应用层注册表承载。
 *
 * @author AIGenerator
 */
public final class TravelPlanningState extends AgentState {

    /**
     * 图状态中的业务上下文键。
     *
     * @author AIGenerator
     */
    public static final String CONTEXT_KEY = "contextKey";

    /**
     * 从LangGraph4j状态数据建立旅行规划状态。
     *
     * @param data 图状态数据
     * @author AIGenerator
     */
    public TravelPlanningState(Map<String, Object> data) {
        super(data);
    }

    /**
     * 返回用于解析框架无关业务上下文的执行键。
     *
     * @return 规划执行键
     * @author AIGenerator
     */
    public String contextKey() {
        return this.<String>value(CONTEXT_KEY)
                .orElseThrow(() -> new IllegalStateException("travel context key missing"));
    }
}
