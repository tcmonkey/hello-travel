package com.hellotravel.application.chat.support;

import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;
import com.hellotravel.model.chat.ChatModelDO;
import com.hellotravel.model.travel.TravelIntentDO;
import com.hellotravel.model.travel.TravelPlanDraftDO;
import com.hellotravel.model.travel.TravelPlanValidationDO;

import java.util.List;
import java.util.Map;

/**
 * 一次旅行回答的应用层上下文；只承载业务状态，不暴露LangChain4j或LangGraph4j类型。
 *
 * @author AIGenerator
 */
public final class TravelConversationContext {

    /**
     * 当前生成任务。
     * @author AIGenerator
     */
    private final ChatRunEntity run;
    /**
     * 本轮用户输入。
     * @author AIGenerator
     */
    private String input = "";
    /**
     * 当前完整问答窗口。
     * @author AIGenerator
     */
    private List<MessageEntity> recent = List.of();
    /**
     * 会话滚动摘要。
     * @author AIGenerator
     */
    private String summary = "";
    /**
     * 用户明确长期记忆。
     * @author AIGenerator
     */
    private String memoryFacts = "";
    /**
     * 有时效性的外部事实。
     * @author AIGenerator
     */
    private String realtimeFacts = "";
    /**
     * 可追溯知识库来源。
     * @author AIGenerator
     */
    private List<Map<String, String>> sources = List.of();
    /**
     * 结构化旅行意图。
     * @author AIGenerator
     */
    private TravelIntentDO intent;
    /**
     * 待校验旅行草稿。
     * @author AIGenerator
     */
    private TravelPlanDraftDO draft;
    /**
     * 需求或草稿校验结果。
     * @author AIGenerator
     */
    private TravelPlanValidationDO validation;
    /**
     * 最终用户回答。
     * @author AIGenerator
     */
    private String answer = "";
    /**
     * 上下文压缩状态。
     * @author AIGenerator
     */
    private String compression = "IDLE";
    /**
     * 保守估算输入量。
     * @author AIGenerator
     */
    private int estimatedTokens;
    /**
     * 供应商实际输入用量。
     * @author AIGenerator
     */
    private Integer actualInputTokens;
    /**
     * 供应商实际输出用量。
     * @author AIGenerator
     */
    private Integer actualOutputTokens;
    /**
     * 当前草稿修订次数。
     * @author AIGenerator
     */
    private int revisionCount;
    /**
     * 最终模型调用证据。
     * @author AIGenerator
     */
    private ChatModelDO modelResponse;

    /**
     * 建立一次旅行回答上下文。
     * @param run 当前生成任务
     * @author AIGenerator
     */
    public TravelConversationContext(ChatRunEntity run) {
        this.run = run;
    }

    /**
     * 返回当前生成任务。
     * @return 生成任务
     * @author AIGenerator
     */
    public ChatRunEntity run() {
        return run;
    }

    /**
     * 返回本轮输入。
     * @return 用户输入
     * @author AIGenerator
     */
    public String input() {
        return input;
    }

    /**
     * 返回近期消息。
     * @return 消息窗口
     * @author AIGenerator
     */
    public List<MessageEntity> recent() {
        return recent;
    }

    /**
     * 返回会话摘要。
     * @return 会话摘要
     * @author AIGenerator
     */
    public String summary() {
        return summary;
    }

    /**
     * 返回长期记忆。
     * @return 长期记忆
     * @author AIGenerator
     */
    public String memoryFacts() {
        return memoryFacts;
    }

    /**
     * 返回实时事实。
     * @return 实时事实
     * @author AIGenerator
     */
    public String realtimeFacts() {
        return realtimeFacts;
    }

    /**
     * 返回资料来源。
     * @return 资料来源
     * @author AIGenerator
     */
    public List<Map<String, String>> sources() {
        return sources;
    }

    /**
     * 返回旅行意图。
     * @return 旅行意图
     * @author AIGenerator
     */
    public TravelIntentDO intent() {
        return intent;
    }

    /**
     * 返回计划草稿。
     * @return 计划草稿
     * @author AIGenerator
     */
    public TravelPlanDraftDO draft() {
        return draft;
    }

    /**
     * 返回校验结果。
     * @return 校验结果
     * @author AIGenerator
     */
    public TravelPlanValidationDO validation() {
        return validation;
    }

    /**
     * 返回最终回答。
     * @return 最终回答
     * @author AIGenerator
     */
    public String answer() {
        return answer;
    }

    /**
     * 返回压缩状态。
     * @return 压缩状态
     * @author AIGenerator
     */
    public String compression() {
        return compression;
    }

    /**
     * 返回输入估算。
     * @return 输入估算
     * @author AIGenerator
     */
    public int estimatedTokens() {
        return estimatedTokens;
    }

    /**
     * 返回实际输入用量。
     * @return 实际输入用量
     * @author AIGenerator
     */
    public Integer actualInputTokens() {
        return actualInputTokens;
    }

    /**
     * 返回实际输出用量。
     * @return 实际输出用量
     * @author AIGenerator
     */
    public Integer actualOutputTokens() {
        return actualOutputTokens;
    }

    /**
     * 返回草稿修订次数。
     * @return 修订次数
     * @author AIGenerator
     */
    public int revisionCount() {
        return revisionCount;
    }

    /**
     * 返回最终模型证据。
     * @return 模型证据
     * @author AIGenerator
     */
    public ChatModelDO modelResponse() {
        return modelResponse;
    }

    /**
     * 加载对话上下文。
     * @param input 用户输入
     * @param recent 近期消息
     * @param summary 摘要
     * @param memoryFacts 长期记忆
     * @author AIGenerator
     */
    public void load(
            String input,
            List<MessageEntity> recent,
            String summary,
            String memoryFacts) {
        // 1. 保存本轮输入和完整问答窗口，后续流程始终使用同一快照。
        this.input = input;
        this.recent = List.copyOf(recent);
        // 2. 规范可空的摘要与长期记忆，避免提示组装出现隐式null文本。
        this.summary = summary == null ? "" : summary;
        this.memoryFacts = memoryFacts == null ? "" : memoryFacts;
    }

    /**
     * 替换压缩后的历史窗口。
     * @param summary 摘要
     * @param recent 近期消息
     * @param compression 压缩状态
     * @author AIGenerator
     */
    public void replaceHistory(String summary, List<MessageEntity> recent, String compression) {
        // 1. 用刚生成的摘要替换旧摘要，并更新仍需保留的原始消息窗口。
        this.summary = summary == null ? "" : summary;
        this.recent = List.copyOf(recent);
        // 2. 保存压缩状态，供上下文使用进度展示和任务检查点读取。
        this.compression = compression;
    }

    /**
     * 保存旅行意图。
     * @param intent 旅行意图
     * @author AIGenerator
     */
    public void intent(TravelIntentDO intent) {
        this.intent = intent;
    }

    /**
     * 保存实时事实。
     * @param realtimeFacts 实时事实
     * @author AIGenerator
     */
    public void realtimeFacts(String realtimeFacts) {
        this.realtimeFacts = realtimeFacts == null ? "" : realtimeFacts;
    }

    /**
     * 保存资料来源。
     * @param sources 资料来源
     * @author AIGenerator
     */
    public void sources(List<Map<String, String>> sources) {
        this.sources = List.copyOf(sources);
    }

    /**
     * 保存计划草稿。
     * @param draft 计划草稿
     * @author AIGenerator
     */
    public void draft(TravelPlanDraftDO draft) {
        this.draft = draft;
    }

    /**
     * 保存校验结果。
     * @param validation 校验结果
     * @author AIGenerator
     */
    public void validation(TravelPlanValidationDO validation) {
        this.validation = validation;
    }

    /**
     * 保存最终回答。
     * @param answer 最终回答
     * @author AIGenerator
     */
    public void answer(String answer) {
        this.answer = answer == null ? "" : answer;
    }

    /**
     * 保存输入估算。
     * @param estimatedTokens 输入估算
     * @author AIGenerator
     */
    public void estimatedTokens(int estimatedTokens) {
        this.estimatedTokens = estimatedTokens;
    }

    /**
     * 保存修订次数。
     * @param revisionCount 修订次数
     * @author AIGenerator
     */
    public void revisionCount(int revisionCount) {
        this.revisionCount = revisionCount;
    }

    /**
     * 保存最终模型证据及实际usage。
     * @param modelResponse 模型响应
     * @author AIGenerator
     */
    public void modelResponse(ChatModelDO modelResponse) {
        // 1. 保存本轮最后一次形成回答的模型证据。
        this.modelResponse = modelResponse;
        // 2. 只有供应商确实返回usage时才同步实际输入和输出用量。
        if (modelResponse != null) {
            this.actualInputTokens = modelResponse.inputTokens();
            this.actualOutputTokens = modelResponse.outputTokens();
        }
    }
}
