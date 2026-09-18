package com.hellotravel.domain.memory.model.value;

/**
 * 上下文预算不变量；估算不是供应商精确token。
 *
 * @param window 应用上下文上限
 * @param estimatedInput 完整输入估算
 * @param outputReserve 输出预留
 * @param safetyReserve 安全预留
 * @author AIGenerator
 */
public record ContextBudgetValue(
        int window, int estimatedInput, int outputReserve, int safetyReserve) {

    /**
     * 校验上下文预算边界，保守估算与实际usage分别记录。
     *
     * @author AIGenerator
     */
    public ContextBudgetValue {
        // 1. 核验总窗口与各项预留边界，避免负数或过小窗口绕过预算判断。
        if (window < 8192 || estimatedInput < 0 || outputReserve < 1 || safetyReserve < 0) {
            throw new IllegalArgumentException("budget");
        }
    }

    /**
     * 判断输入及预留是否能放入应用上下文上限。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public boolean fits() {
        return (long) estimatedInput + outputReserve + safetyReserve <= window;
    }

    /**
     * 判断是否达到摘要压缩阈值。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public boolean needsCompression() {
        return (long) estimatedInput + outputReserve > window * 0.7;
    }

    /**
     * 使用UTF8字节数给出保守输入预算。
     *
     * @author AIGenerator
     * @param text 有界文本内容
     * @return 当前操作的业务结果
     */
    public static int estimate(String text) {
        return text == null
                ? 0
                : Math.addExact(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, 64);
    }

    /**
     * 定义已选择的模型限制，初始输入为零。
     *
     * @param window 本次转换的window快照
     * @param outputReserve 本次转换的outputReserve快照
     * @param safetyReserve 本次转换的safetyReserve快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static ContextBudgetValue policy(int window, int outputReserve, int safetyReserve) {
        return new ContextBudgetValue(window, 0, outputReserve, safetyReserve);
    }

    /**
     * 在同一模型限制下替换完整输入估算。
     *
     * @param estimatedInput 本次转换的estimatedInput快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ContextBudgetValue withInput(int estimatedInput) {
        return new ContextBudgetValue(window, estimatedInput, outputReserve, safetyReserve);
    }

    /**
     * 为本次能力调用选择实际输出预留，窗口及安全预留保持不变。
     *
     * @param output 当前能力的输出上限
     * @return 本次完整预算
     * @author AIGenerator
     */
    public ContextBudgetValue withOutput(int output) {
        return new ContextBudgetValue(window, estimatedInput, output, safetyReserve);
    }
}
