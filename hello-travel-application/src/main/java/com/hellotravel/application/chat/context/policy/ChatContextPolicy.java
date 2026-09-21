package com.hellotravel.application.chat.context.policy;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;
import com.hellotravel.model.chat.ChatModelStage;

import org.springframework.core.env.Environment;
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
     * 默认摘要输入预算，供未覆盖模型策略的运行环境使用。
     *
     * @author AIGenerator
     */
    private static final int DEFAULT_COMPRESSION_INPUT_LIMIT = 96_000;

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
     * @param environment 对话模型预算配置源
     * @author AIGenerator
     */
    public ChatContextPolicy(Environment environment) {
        // 1. 读取统一模型窗口及回答、安全预留，由领域值对象校验基本边界。
        this.limits =
                ContextBudgetValue.policy(
                        environment.getProperty(
                                "travel.model.context-window", Integer.class, 131072),
                        environment.getProperty(
                                "travel.model.output-reserve", Integer.class, 8192),
                        environment.getProperty(
                                "travel.model.safety-reserve", Integer.class, 4096));
        // 2. 单独限制摘要输入，防止辅助任务占满完整模型窗口。
        Integer configuredCompressionLimit =
                environment.getProperty(
                        "travel.model.compression-input-limit", Integer.class);
        // 2. 未显式设置摘要预算时，随较小的自定义窗口收敛；显式值仍严格拒绝越界。
        this.compressionInputLimit =
                configuredCompressionLimit == null
                        ? Math.min(DEFAULT_COMPRESSION_INPUT_LIMIT, availableInputCapacity())
                        : configuredCompressionLimit;
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
     * 计算当前策略中可分配给输入的最大容量。
     *
     * @return 除去回答和安全预留后的输入容量
     * @author AIGenerator
     */
    private int availableInputCapacity() {
        // 1. 所有字段已由领域值对象校验，减法不可能产生负数。
        return limits.window() - limits.outputReserve() - limits.safetyReserve();
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
