package com.hellotravel.application.auth.support;

import com.hellotravel.application.auth.assembler.AuthWriteApplicationAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.service.AuthDomainService;

import org.springframework.stereotype.Component;

/**
 * 认证事务内部的领域写入协作；失败中断并交由所属应用入口捕获。
 *
 * @author AIGenerator
 */
@Component
public final class AuthWrites {

    private final AuthDomainService service;
    private final AuthWriteApplicationAssembler authWriteApplicationAssembler;

    public AuthWrites(
            AuthDomainService service,
            AuthWriteApplicationAssembler authWriteApplicationAssembler) {
        this.service = service;
        this.authWriteApplicationAssembler = authWriteApplicationAssembler;
    }

    /**
     * 校验UserAccount完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveUserAccount(UserAccountAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveUserAccount(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验Device完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveDevice(DeviceAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveDevice(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验LoginSession完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveLoginSession(LoginSessionAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveLoginSession(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验EmailChallenge完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveEmailChallenge(EmailChallengeAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveEmailChallenge(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验RefreshReceipt完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveRefreshReceipt(RefreshReceiptAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveRefreshReceipt(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 清理RefreshReceipt的指定失效记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    public Boolean removeRefreshReceipt(Long id) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = authWriteApplicationAssembler.removeRefreshReceipt(id);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.removeRefreshReceipt(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }
}
