package com.hellotravel.application.auth;

import com.hellotravel.application.auth.adaptor.SecurityAdaptor;
import com.hellotravel.application.auth.assembler.AuthAppAssembler;
import com.hellotravel.application.auth.assembler.AuthDomainParamAssembler;
import com.hellotravel.application.auth.assembler.SecurityCommandAppAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import org.springframework.stereotype.Component;

/**
 * REGISTER认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class RegisterAppService extends AuthCredentialSupport implements AuthActionHandler {

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
  public RegisterAppService(
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
    return "REGISTER";
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
    limit(command, "register:" + address);
    // 3. 准备已核验的邮箱证明，供短事务消费。
    EmailChallengeEntity verified = proof(command, "REGISTER", address);
    String hash = hashPassword(command.password());
    // 4. 进入受控事务处理，结果与回滚责任保持清晰。
    transactions.plain(
        () -> {
          // 1. 在同一业务事务内消费已验证挑战，业务失败时一起回滚。
          consume(verified);
          // 2. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          UserAccountEntity created =
              UserAccountAggregate.registered(
                      address,
                      hash,
                      now(),
                      java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                      java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                  .entity();
          // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveUserAccount(
                          authDomainParamAssembler.userAccount(new UserAccountAggregate(created))))
                  .saved());
          // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return null;
        });
    // 5. 取得新账号的持久化快照，供本段后续处理使用。
    UserAccountEntity user = account(address);
    // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
    return authAppAssembler.account(user);
  }
}
