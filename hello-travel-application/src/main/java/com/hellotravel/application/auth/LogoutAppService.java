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
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import org.springframework.stereotype.Component;

/**
 * LOGOUT认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class LogoutAppService extends AuthCredentialSupport implements AuthActionHandler {

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
  public LogoutAppService(
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
    return "LOGOUT";
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
    // 1. 取得当前请求的认证主体，供本段后续处理使用。
    AuthAppResult principal = authenticated(command);
    // 2. 进入受控事务处理，结果与回滚责任保持清晰。
    transactions.mutate(
        principal.userId(),
        current -> {
          // 1. 按可信内部标识读取登录会话当前快照。
          LoginSessionEntity stored =
              loginSessionRepository.findById(principal.sessionId()).entity();
          // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveLoginSession(
                          authDomainParamAssembler.loginSession(
                              new LoginSessionAggregate(stored).revoke("LOGOUT"))))
                  .saved());
          // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              current,
              "session.revoked",
              stored.publicId(),
              stored.version() + 1,
              stored.publicId(),
              "{}");
          // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return null;
        });
    // 3. 返回本段实际处理结果，保持本层输出契约。
    return principal;
  }
}
