package com.hellotravel.application.auth.assembler;

import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.param.DeviceWriteParam;
import com.hellotravel.domain.auth.model.param.EmailChallengeWriteParam;
import com.hellotravel.domain.auth.model.param.LoginSessionWriteParam;
import com.hellotravel.domain.auth.model.param.RefreshReceiptRemoveParam;
import com.hellotravel.domain.auth.model.param.RefreshReceiptWriteParam;
import com.hellotravel.domain.auth.model.param.UserAccountWriteParam;

import org.springframework.stereotype.Component;

/**
 * 认证应用写入到本域参数的转换，不承担其他业务的映射。
 *
 * @author AIGenerator
 */
@Component
public final class AuthWriteAppAssembler {

    /**
     * 投影UserAccount写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public UserAccountWriteParam write(UserAccountAggregate aggregate) {
        return new UserAccountWriteParam(aggregate);
    }

    /**
     * 投影Device写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DeviceWriteParam write(DeviceAggregate aggregate) {
        return new DeviceWriteParam(aggregate);
    }

    /**
     * 投影LoginSession写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public LoginSessionWriteParam write(LoginSessionAggregate aggregate) {
        return new LoginSessionWriteParam(aggregate);
    }

    /**
     * 投影EmailChallenge写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public EmailChallengeWriteParam write(EmailChallengeAggregate aggregate) {
        return new EmailChallengeWriteParam(aggregate);
    }

    /**
     * 投影RefreshReceipt写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public RefreshReceiptWriteParam write(RefreshReceiptAggregate aggregate) {
        return new RefreshReceiptWriteParam(aggregate);
    }

    /**
     * 投影RefreshReceipt删除参数。
     *
     * @param id 本次转换的id快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public RefreshReceiptRemoveParam removeRefreshReceipt(Long id) {
        return new RefreshReceiptRemoveParam(id);
    }
}
