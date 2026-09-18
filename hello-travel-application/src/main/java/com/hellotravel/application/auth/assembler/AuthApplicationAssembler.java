package com.hellotravel.application.auth.assembler;

import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;

import org.springframework.stereotype.Component;

/**
 * 认证持久快照到应用结果的用途映射。
 *
 * @author AIGenerator
 */
@Component
public final class AuthApplicationAssembler {

    /**
     * 返回已受理的邮箱挑战，不构造虚假会话。
     *
     * @param challengeId 本次转换的challengeId快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthResult challenge(String challengeId) {
        return new AuthResult(null, null, null, null, null, null, null, null, challengeId);
    }

    /**
     * 投影已完成账号操作的账号视图。
     *
     * @param account 本次转换的account快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthResult account(UserAccountEntity account) {
        return session(account, null, null, null, null);
    }

    /**
     * 投影鉴权通过的会话，不轮换或暴露新凭据。
     *
     * @param account 本次转换的account快照
     * @param session 本次转换的session快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthResult checked(UserAccountEntity account, LoginSessionEntity session) {
        return session(account, session, null, null, null);
    }

    /**
     * 投影已签发或轮换的会话凭据，刷新令牌仅供协议Cookie处理。
     *
     * @param account 本次转换的account快照
     * @param session 本次转换的session快照
     * @param access 本次转换的access快照
     * @param refresh 本次转换的refresh快照
     * @param csrf 本次转换的csrf快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public AuthResult session(
            UserAccountEntity account,
            LoginSessionEntity session,
            String access,
            String refresh,
            String csrf) {
        return new AuthResult(
                account.id(),
                account.publicId(),
                account.emailNormalized(),
                session == null ? null : session.publicId(),
                session == null ? null : session.id(),
                access,
                refresh,
                csrf,
                null);
    }
}
