package com.hellotravel.adaptor.auth.output.security.converter;

import com.hellotravel.model.security.SecurityDO;

import org.springframework.stereotype.Component;

/**
 * 密码与签名能力的结果投影。
 *
 * @author AIGenerator
 */
@Component
public final class SecurityOutputConverter {

    /**
     * 转换已生成的受保护值。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityDO value(String value) {
        return new SecurityDO(value, true);
    }

    /**
     * 转换验证结果，不携带原始证明。
     *
     * @param valid 本次转换的valid快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SecurityDO verification(boolean valid) {
        return new SecurityDO(null, valid);
    }
}
