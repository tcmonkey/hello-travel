package com.hellotravel.model.chat;

/**
 * 对话域内可审计的模型调用阶段。
 *
 * @author AIGenerator
 */
public enum ChatModelStage {

    /**
     * 结构化旅行意图识别。
     *
     * @author AIGenerator
     */
    INTENT,

    /**
     * 普通对话或带事实的对话回答。
     *
     * @author AIGenerator
     */
    DIALOGUE,

    /**
     * 第一版旅行计划草稿。
     *
     * @author AIGenerator
     */
    PLAN_DRAFT,

    /**
     * 按领域违规项修订旅行计划。
     *
     * @author AIGenerator
     */
    PLAN_REVISION,

    /**
     * 会话滚动摘要压缩。
     *
     * @author AIGenerator
     */
    MEMORY_SUMMARY,

    /**
     * 用户明确长期事实提取。
     *
     * @author AIGenerator
     */
    MEMORY_EXTRACTION
}
