package com.hellotravel.application.auth.assembler;

import com.hellotravel.application.auth.command.MailCommand;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.model.security.SecurityDO;

import org.springframework.stereotype.Component;

/**
 * 验证码投递命令映射。
 *
 * @author AIGenerator
 */
@Component
public final class MailCommandAppAssembler {

    /**
     * 绑定持久挑战与临时解密结果，原始验证码不写日志。
     *
     * @param challenge 本次转换的challenge快照
     * @param decrypted 本次转换的decrypted快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MailCommand challenge(EmailChallengeEntity challenge, SecurityDO decrypted) {
        return new MailCommand(
                challenge.emailNormalized(), decrypted.value(), challenge.publicId());
    }
}
