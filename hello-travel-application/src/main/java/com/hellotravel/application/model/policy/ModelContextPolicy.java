package com.hellotravel.application.model.policy;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.domain.memory.model.value.ContextBudgetValue;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 共享模型预算策略，领域值对象判断容量及压缩，配置源决定选用窗口。
 *
 * @author AIGenerator
 */
@Component
public final class ModelContextPolicy {
    /**
     * 当前选定模型的不可变窗口及输出、安全预留。
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
     * 选择模型上下文预算并核验配置，错误配置在启动时失败。
     *
     * @param environment 模型预算配置源
     * @author AIGenerator
     */
    public ModelContextPolicy(Environment environment) {
        // 1. 读取配置模型预算，领域值对象负责边界不变量。
        this.limits =
                ContextBudgetValue.policy(
                        environment.getProperty(
                                "travel.model.context-window", Integer.class, 32768),
                        environment.getProperty("travel.model.output-reserve", Integer.class, 4096),
                        environment.getProperty(
                                "travel.model.safety-reserve", Integer.class, 4096));
        // 2. 核验摘要自身输入上限，不允许摘要调用绕过模型窗口。
        this.compressionInputLimit =
                environment.getProperty(
                        "travel.model.compression-input-limit", Integer.class, 20000);
        if (compressionInputLimit < 1
                || !limits.withInput(compressionInputLimit).fits()
                || !limits.fits()) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
    }

    /**
     * 为当前输入估算取得已配置且可验证的领域预算。
     *
     * @param estimatedInput 本次转换的estimatedInput快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ContextBudgetValue budget(int estimatedInput) {
        return limits.withInput(estimatedInput);
    }

    /**
     * 取得回答服务实际允许的输出预留。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public int outputReserve() {
        return limits.outputReserve();
    }

    /**
     * 取得摘要任务自身允许的输入估算上限。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public int compressionInputLimit() {
        return compressionInputLimit;
    }

    /**
     * 辅助模型任务的保守输出上限。
     *
     * @author AIGenerator
     */
    private static final int AUXILIARY_OUTPUT_LIMIT = 2048;

    /**
     * 选择本次能力的输出上限，辅助任务不越过配置的回答预留。
     *
     * @param stage 当前模型能力阶段
     * @return 当前能力输出上限
     * @author AIGenerator
     */
    public int outputLimit(String stage) {
        return "ANSWER".equals(stage)
                ? outputReserve()
                : Math.min(AUXILIARY_OUTPUT_LIMIT, outputReserve());
    }

    /**
     * 在模型IO前验证完整聊天输入预算，辅助调用也不能跳过窗口限制。
     *
     * @param command 完整模型输入
     * @return 本次调用是否满足所选模型预算
     * @author AIGenerator
     */
    public boolean accepts(ModelCommand command) {
        // 1. 对可信系统规则及所有历史消息计算相同的保守估算。
        long input = ContextBudgetValue.estimate(command.system());
        for (var message : command.messages()) {
            input += ContextBudgetValue.estimate(message.text());
        }
        // 2. 拒绝整数溢出，按本次实际输出上限核验领域预算。
        return input <= Integer.MAX_VALUE
                && budget((int) input).withOutput(outputLimit(command.action())).fits();
    }
}
