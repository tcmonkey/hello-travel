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
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.util.JsonUtil;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * CHALLENGE认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class ChallengeAppService extends AuthCredentialSupport implements AuthActionHandler {

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
  public ChallengeAppService(
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
    return "CHALLENGE";
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
    // 2. 仅接受统一登录和密码设置/重置两种验证码用途。
    if (!Set.of("LOGIN", "RESET_PASSWORD").contains(command.purpose())) {
      throw new ApplicationException(ApplicationErrorCode.INVALID);
    }
    // 3. 统一登录码不按账号是否存在分支，避免首次用户被拒绝或泄露账号状态。
    // 4. 核对安全端口返回的验证或限流结果，失败提前中止。
    if (!secure(securityCommandAppAssembler.mailAvailable()).valid()) {
      throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
    }
    // 5. 执行当前主体及用途的频控，超限提前中止。
    limit(command, "issue:" + address + ":" + command.purpose());
    // 6. 执行当前主体及用途的频控，超限提前中止。
    limit(command, "issue-ip");
    // 7. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
    String id = Ids.next();
    String code = secure(securityCommandAppAssembler.otp()).value();
    byte[] hmac =
        Base64.getDecoder()
            .decode(
                secure(
                        securityCommandAppAssembler.hmac(
                            code, id + "|" + address + "|" + command.purpose()))
                    .value());
    byte[] encrypted =
        Base64.getDecoder().decode(secure(securityCommandAppAssembler.encrypt(code, id)).value());
    // 8. 进入受控事务处理，结果与回滚责任保持清晰。
    transactions.plain(
        () -> {
          // 1. 读取邮箱验证码，按当前用例条件限定查询窗口。
          var previous =
              emailChallengeRepository.query(
                  QueryValue.all("id", 10)
                      .where("email_normalized", "EQ", address)
                      .where("purpose", "EQ", command.purpose())
                      .where("status", "IN", List.of("ISSUED", "PENDING_SEND")));
          // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
          for (var old : previous) {
            Transactions.require(
                ApplicationFailures.required(
                        authDomainService.saveEmailChallenge(
                            authDomainParamAssembler.emailChallenge(
                                new EmailChallengeAggregate(old.entity()).delivery("REVOKED"))))
                    .saved());
          }
          // 3. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          EmailChallengeEntity created =
              EmailChallengeAggregate.pendingDelivery(
                      id,
                      address,
                      command.purpose(),
                      hmac,
                      encrypted,
                      now().plusMinutes(5),
                      java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                      java.time.LocalDateTime.now(java.time.ZoneOffset.UTC))
                  .entity();
          // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveEmailChallenge(
                          authDomainParamAssembler.emailChallenge(
                              new EmailChallengeAggregate(created))))
                  .saved());
          // 5. 同事务登记可恢复后台任务，外部调用在提交之后执行。
          events.outbox(null, "EMAIL", id, JsonUtil.encode(Map.of("challengeId", id)));
          // 6. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
          return null;
        });
    // 9. 返回本段实际处理结果，保持本层输出契约。
    return authAppAssembler.challenge(id);
  }
}
