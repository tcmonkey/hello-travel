package com.hellotravel.application.auth.assembler;

import com.hellotravel.application.auth.result.ChallengeStatusAppResult;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;

import org.springframework.stereotype.Component;

/**
 * 邮箱验证码实体到公开状态结果的应用层转换器。
 *
 * @author AIGenerator
 */
@Component
public final class ChallengeStatusAppAssembler {

    /**
     * 投影不含邮箱、验证码和投递密文的状态结果。
     *
     * @param challenge 已读取的验证码实体
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChallengeStatusAppResult status(EmailChallengeEntity challenge) {
        // 1. 仅投影公开请求标识和状态，私密认证载荷不离开应用边界。
        return new ChallengeStatusAppResult(challenge.publicId(), challenge.status());
    }
}
