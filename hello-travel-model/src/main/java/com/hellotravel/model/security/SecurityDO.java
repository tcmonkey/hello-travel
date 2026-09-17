package com.hellotravel.model.security;

/**
 * 安全能力结果。
 *
 * @param value 处理后文本
 * @param valid 验证结果
 * @author AIGenerator
 */
public record SecurityDO(String value, boolean valid) {

    /**
     * 阻止诊断输出泄漏安全值或模型正文。
     *
     * @return 脱敏类型
     * @author AIGenerator
     */
    @Override
    public String toString() {
        return "SecurityDO{redacted}";
    }
}
