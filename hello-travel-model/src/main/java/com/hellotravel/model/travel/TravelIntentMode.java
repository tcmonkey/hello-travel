package com.hellotravel.model.travel;

/**
 * 旅行对话在应用层允许进入的受限业务分支。
 *
 * @author AIGenerator
 */
public enum TravelIntentMode {

    /**
     * 不需要外部实时事实和逐日规划的普通旅行对话。
     *
     * @author AIGenerator
     */
    DIALOGUE,

    /**
     * 需要天气、路线或知识证据的事实问答。
     *
     * @author AIGenerator
     */
    FACT_QUERY,

    /**
     * 需要多步骤校验、生成和修订的旅行规划。
     *
     * @author AIGenerator
     */
    PLANNING
}
