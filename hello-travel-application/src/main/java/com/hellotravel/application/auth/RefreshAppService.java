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
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.auth.service.AuthDomainService;
import com.hellotravel.domain.query.model.value.QueryValue;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

/**
 * REFRESH认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class RefreshAppService extends AuthCredentialSupport implements AuthActionHandler {

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
  public RefreshAppService(
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
    return "REFRESH";
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
    // 1. 保留当前来源或状态快照，后续核对并发变更与重复执行。
    LoginSessionEntity snapshot;
    // 2. 在异常捕获或资源释放边界内完成本段处理，失败不得伪装为成功。
    try {
      snapshot = session(command.refreshToken(), true);
    } catch (ApplicationException exception) {
      if (command.refreshToken() != null && command.refreshToken().length() <= 200) {
        var receipts =
            refreshReceiptRepository.query(
                QueryValue.all("id", 1)
                    .where("token_hash", "EQ", Ids.hash(command.refreshToken()))
                    .where("expires_at", "GT", now()));
        if (!receipts.isEmpty()) {
          var prior =
              loginSessionRepository.findById(receipts.get(0).entity().sessionId()).entity();
          if (!prior.publicId().equals(command.expectedSid())) {
            throw new ApplicationException(ApplicationErrorCode.SESSION_REPLACED);
          }
          transactions.mutate(
              prior.userId(),
              owner -> {
                // 1. 按可信内部标识读取登录会话当前快照。
                var current = loginSessionRepository.findById(prior.id()).entity();
                // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                Transactions.require(
                    ApplicationFailures.required(
                            authDomainService.saveLoginSession(
                                authDomainParamAssembler.loginSession(
                                    new LoginSessionAggregate(current).revoke("REFRESH_REPLAY"))))
                        .saved());
                // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                events.append(
                    owner,
                    "session.revoked",
                    current.publicId(),
                    current.version() + 1,
                    current.publicId(),
                    "{}");
                // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                return null;
              });
        }
      }
      throw exception;
    }
    // 3. 取得凭据对应的已验证账号，供本段后续处理使用。
    UserAccountEntity owner = valid(snapshot, command.expectedSid(), true);
    // 4. 核对CSRF证明与当前会话凭据，防止跨站或旧页面冒用。
    if (command.csrf() == null
        || !MessageDigest.isEqual(snapshot.csrfTokenHash(), Ids.hash(command.csrf()))) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 5. 生成本次签发或轮换的随机访问、刷新及CSRF凭据。
    String access = Ids.token(), refresh = Ids.token(), csrf = Ids.token();
    // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
    return transactions.mutate(
        owner.id(),
        current -> {
          // 1. 按可信内部标识读取登录会话当前快照。
          LoginSessionEntity stored = loginSessionRepository.findById(snapshot.id()).entity();
          // 2. 执行valid职责步骤，并把失败交给所属事务或入口处理。
          valid(stored, command.expectedSid(), true);
          // 3. 刷新证明与已加载快照不一致时拒绝轮换，阻断消费后的重放。
          if (!MessageDigest.isEqual(stored.refreshTokenHash(), Ids.hash(command.refreshToken()))) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
          }
          // 4. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          var receipt =
              RefreshReceiptAggregate.consumed(
                      stored.userId(),
                      stored.id(),
                      Ids.hash(command.refreshToken()),
                      now(),
                      stored.refreshExpiresAt())
                  .entity();
          // 5. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveRefreshReceipt(
                          authDomainParamAssembler.refreshReceipt(
                              new RefreshReceiptAggregate(receipt))))
                  .saved());
          // 6. 通过领域聚合语义准备业务快照，固定状态由实体封装。
          LoginSessionEntity rotated =
              new LoginSessionAggregate(stored)
                  .rotate(
                      Ids.hash(access), Ids.hash(refresh), Ids.hash(csrf), now().plusMinutes(15))
                  .entity();
          // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveLoginSession(
                          authDomainParamAssembler.loginSession(
                              new LoginSessionAggregate(rotated))))
                  .saved());
          // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(
              current, "session.refreshed", stored.publicId(), stored.version() + 1, null, "{}");
          // 9. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
          return authAppAssembler.session(current, rotated, access, refresh, csrf);
        });
  }
}
