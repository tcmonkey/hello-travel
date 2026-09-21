package com.hellotravel.application.chat.context.policy;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对话上下文窗口策略，统一回答、意图与记忆任务的容量边界。
 *
 * @author AIGenerator
 */
@Component
public final class ChatContextPolicy {

    /**
     * 辅助模型任务的保守输出上限。
     *
     * @author AIGenerator
     */
    private static final int AUXILIARY_OUTPUT_LIMIT = 2048;

    /**
     * 当前对话模型的不可变窗口及预留。
     *
     * @author AIGenerator
     */
    private final ContextBudgetValue limits;

    /**
     * 摘要任务自身允许的输入估算上限。
     *
     * @author AIGenerator
     */
    private final int compressionInputLimit;

    /**
     * 从运行配置建立不可变上下文窗口，错误配置在启动时失败。
     *
     * @param contextWindow 应用上下文窗口
     * @param outputReserve 回答输出预留
     * @param safetyReserve 安全预留
     * @param compressionInputLimit 摘要输入上限
     * @author AIGenerator
     */
    public ChatContextPolicy(
            @Value("\u0024{travel.model.context-window}") int contextWindow,
            @Value("\u0024{travel.model.output-reserve}") int outputReserve,
            @Value("\u0024{travel.model.safety-reserve}") int safetyReserve,
            @Value("\u0024{travel.model.compression-input-limit}") int compressionInputLimit) {
        // 1. 读取统一模型窗口及回答、安全预留，由领域值对象校验基本边界。
        this.limits =
                ContextBudgetValue.policy(
                        contextWindow, outputReserve, safetyReserve);
        // 2. 使用YAML给出的摘要上限；配置错误在启动阶段明确拒绝。
        this.compressionInputLimit = compressionInputLimit;
        // 3. 阻止摘要任务挤占回答和安全预留，避免运行中才发现配置不可执行。
        if (compressionInputLimit < 1
                || !limits.withInput(compressionInputLimit).fits()
                || !limits.fits()) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
    }

    /**
     * 取得带本次输入估算的上下文预算。
     *
     * @param estimatedInput 本次输入估算
     * @return 不可变预算值
     * @author AIGenerator
     */
    public ContextBudgetValue budget(int estimatedInput) {
        return limits.withInput(estimatedInput);
    }

    /**
     * 取得回答预留。
     *
     * @return 输出预留
     * @author AIGenerator
     */
    public int outputReserve() {
        return limits.outputReserve();
    }

    /**
     * 取得摘要任务输入上限。
     *
     * @return 摘要输入估算上限
     * @author AIGenerator
     */
    public int compressionInputLimit() {
        return compressionInputLimit;
    }

    /**
     * 选择当前对话能力的输出上限。
     *
     * @param stage 对话能力阶段
     * @return 输出上限
     * @author AIGenerator
     */
    public int outputLimit(ChatModelStage stage) {
        return stage == ChatModelStage.DIALOGUE
                ? outputReserve()
                : Math.min(AUXILIARY_OUTPUT_LIMIT, outputReserve());
    }

    /**
     * 在外部调用前核验可信规则与资料正文的完整预算。
     *
     * @param instructions 可信内部规则
     * @param inputs 有界资料正文
     * @param stage 对话能力阶段
     * @return 是否允许调用
     * @author AIGenerator
     */
    public boolean accepts(
            String instructions, List<String> inputs, ChatModelStage stage) {
        // 1. 对可信规则和全部资料使用相同的保守估算。
        long amount = ContextBudgetValue.estimate(instructions);
        for (String input : inputs) {
            amount += ContextBudgetValue.estimate(input);
        }
        // 2. 拒绝整数溢出，并按本能力的真实输出预留核验窗口。
        return amount <= Integer.MAX_VALUE
                && budget((int) amount).withOutput(outputLimit(stage)).fits();
    }
}
