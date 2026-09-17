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
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public ContextBudgetValue {
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
}
