package com.hellotravel.application.auth;

import com.hellotravel.application.auth.adaptor.SecurityAdaptor;
import com.hellotravel.application.auth.assembler.AuthAppAssembler;
import com.hellotravel.application.auth.assembler.AuthDomainParamAssembler;
import com.hellotravel.application.auth.assembler.SecurityCommandAppAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import org.springframework.stereotype.Component;

/**
 * RESET认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class ResetPasswordAppService extends AuthCredentialSupport
    implements AuthActionHandler {

  /**
   * 注入该业务动作所需的受控协作。
   *
   * @param authDomainService 注入的受控协作。
   * @param authDomainParamAssembler 注入的受控协作。
   * @param userAccountRepository 注入的受控协作。
   * @param deviceRepository 注入的受控协作。
   * @param loginSessionRepository 注入的受控协作。
   * @param emailChallengeRepository 注入的受控协作。
   * @param refreshReceiptRepository 注入的受控协作。
   * @param transactions 注入的受控协作。
   * @param events 注入的受控协作。
   * @param security 注入的受控协作。
   * @param authAppAssembler 注入的受控协作。
   * @param securityCommandAppAssembler 注入的受控协作。
   * @author AIGenerator
   */
  public ResetPasswordAppService(
      AuthDomainService authDomainService,
      AuthDomainParamAssembler authDomainParamAssembler,
      UserAccountRepository userAccountRepository,
      DeviceRepository deviceRepository,
      LoginSessionRepository loginSessionRepository,
      EmailChallengeRepository emailChallengeRepository,
      RefreshReceiptRepository refreshReceiptRepository,
      Transactions transactions,
      SyncEventPublisher events,
      SecurityAdaptor security,
      AuthAppAssembler authAppAssembler,
      SecurityCommandAppAssembler securityCommandAppAssembler) {
    super(
        authDomainService,
        authDomainParamAssembler,
        userAccountRepository,
        deviceRepository,
        loginSessionRepository,
        emailChallengeRepository,
        refreshReceiptRepository,
        transactions,
        events,
        security,
        authAppAssembler,
        securityCommandAppAssembler);
  }

  /**
   * 返回本类唯一处理的业务动作。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  @Override
  public String action() {
    return "RESET";
  }

  /**
   * 执行本类唯一的业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 当前动作的应用结果
   * @author AIGenerator
   */
  @Override
  public AuthAppResult execute(AuthCommand command) {
    // 1. 规范化邮箱并限制输入长度。
    String address = email(command.email());
    // 2. 执行当前主体及用途的频控，超限提前中止。
    limit(command, "reset:" + address);
    // 3. 准备已核验的邮箱证明，供短事务消费。
    EmailChallengeEntity verified = proof(command, "RESET_PASSWORD", address);
    String hash = hashPassword(command.password());
    UserAccountEntity owner = account(address);
    // 4. 核对输入或读取结果的存在性，失败中止当前处理。
    if (owner == null) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 5. 进入受控事务处理，结果与回滚责任保持清晰。
    transactions.mutate(
        owner.id(),
        current -> {
          // 1. 在同一业务事务内消费已验证挑战，业务失败时一起回滚。
          consume(verified);
          // 2. 按可信内部标识读取账号当前快照。
          UserAccountEntity locked = userAccountRepository.findById(current.id()).entity();
          // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveUserAccount(
                          authDomainParamAssembler.userAccount(
                              new UserAccountAggregate(locked).resetPassword(hash))))
                  .saved());
          // 4. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
          for (var active :
              loginSessionRepository.query(
                  QueryValue.all("id", 1000)
                      .where("user_id", "EQ", current.id())
                      .where("status", "EQ", "ACTIVE"))) {
            Transactions.require(
                ApplicationFailures.required(
                        authDomainService.saveLoginSession(
                            authDomainParamAssembler.loginSession(
                                new LoginSessionAggregate(active.entity())
                                    .revoke("PASSWORD_RESET"))))
                    .saved());
          }
          // 5. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              current,
              "account.sessions_revoked",
              owner.publicId(),
              locked.version() + 1,
              null,
              "{}");
          // 6. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return null;
        });
    // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
    return authAppAssembler.account(owner);
  }
}
