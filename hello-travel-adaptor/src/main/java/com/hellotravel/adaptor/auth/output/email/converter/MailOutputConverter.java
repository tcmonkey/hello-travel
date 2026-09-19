package com.hellotravel.adaptor.auth.output.email.converter;

import com.hellotravel.model.auth.MailDO;

import org.springframework.stereotype.Component;

/**
 * 邮件服务返回值到内部能力结果的转换。
 *
 * @author AIGenerator
 */
@Component
public final class MailOutputConverter {

    /**
     * 返回已被邮件服务接受的投递结果。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MailDO accepted() {
        return new MailDO(true);
    }
}
