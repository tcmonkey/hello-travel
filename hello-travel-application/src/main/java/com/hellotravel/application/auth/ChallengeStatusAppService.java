package com.hellotravel.application.auth;

import com.hellotravel.application.auth.result.ChallengeStatusAppResult;
import com.hellotravel.application.auth.assembler.ChallengeStatusAppAssembler;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Service;

/**
 * 查询验证码投递状态的应用服务，不暴露邮箱、验证码或投递载荷。
 *
 * @author AIGenerator
 */
@Service
public final class ChallengeStatusAppService {

    private final EmailChallengeRepository emailChallengeRepository;
    private final ChallengeStatusAppAssembler challengeStatusAppAssembler;

    public ChallengeStatusAppService(
            EmailChallengeRepository emailChallengeRepository,
            ChallengeStatusAppAssembler challengeStatusAppAssembler) {
        this.emailChallengeRepository = emailChallengeRepository;
        this.challengeStatusAppAssembler = challengeStatusAppAssembler;
    }

    /**
     * 查询指定验证码请求的投递状态。
     *
     * @param challengeId 验证请求公开标识
     * @return 不含敏感载荷的状态结果
     * @author AIGenerator
     */
    public Result<ChallengeStatusAppResult> query(String challengeId) {
        try {
            // 1. 校验公开标识格式，避免无界查询进入持久化层。
            if (challengeId == null || !challengeId.matches("[0-9A-HJKMNP-TV-Z]{26}")) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 按公开标识读取当前验证码状态，不读取收件邮箱和加密载荷。
            var rows =
                    emailChallengeRepository.query(
                            QueryValue.all("id", 1).where("public_id", "EQ", challengeId));
            if (rows.isEmpty()) {
                throw new ApplicationException(ApplicationErrorCode.NOT_FOUND);
            }
            // 3. 仅返回请求标识和投递状态，保留验证码及邮箱的私有边界。
            return Result.success(challengeStatusAppAssembler.status(rows.get(0).entity()));
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
