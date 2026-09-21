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
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
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
 * LOGIN认证或业务动作的应用服务。
 *
 * @author AIGenerator
 */
@Component
public final class LoginAppService extends AuthCredentialSupport implements AuthActionHandler {

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
  public LoginAppService(
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
    return "LOGIN";
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
    limit(command, "login:" + address);
    // 3. 固定登录方式和当前账号快照，验证码登录允许账号尚不存在。
    boolean codeLogin = command.challengeId() != null && !command.challengeId().isBlank();
    UserAccountEntity snapshot = account(address);
    // 4. 验证码路径先确认邮箱归属，密码路径使用等成本校验保护账号存在性。
    if (!codeLogin) {
      boolean valid = secure(securityCommandAppAssembler.passwordProof(command, snapshot)).valid();
      if (!valid || snapshot == null || !"ACTIVE".equals(snapshot.status())) {
        throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
      }
      return issueForExistingAccount(command, snapshot, null, true);
    }
    EmailChallengeEntity verified = proof(command, "LOGIN", address);
    // 5. 已有账号直接签发会话；首次有效验证码在同一事务中创建账号并签发会话。
    if (snapshot != null) {
      return issueForExistingAccount(command, snapshot, verified, false);
    }
    UserAccountAggregate initial =
        UserAccountAggregate.emailVerified(
            address,
            now(),
            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
            java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
    return transactions.ensureAccountAndMutate(
        initial,
        current -> issueSession(command, current, verified, null, false));
  }

  /**
   * 为已有账号在版本受控事务内签发当前浏览器会话。
   *
   * @author AIGenerator
   * @param command 已由输入层转换的命令
   * @param snapshot 验证前读取的账号快照
   * @param codeProof 已验证的登录验证码；密码登录为空
   * @param passwordLogin 是否按密码快照复核
   * @return 当前动作的应用结果
   */
  private AuthAppResult issueForExistingAccount(
      AuthCommand command,
      UserAccountEntity snapshot,
      EmailChallengeEntity codeProof,
      boolean passwordLogin) {
    // 1. 通过账号提交序列串行化当前设备会话替换与同步事件。
    return transactions.mutate(
        snapshot.id(),
        current -> issueSession(command, current, codeProof, snapshot, passwordLogin));
  }

  /**
   * 在调用方已开启的短事务内消费验证码、替换本设备会话并组装认证结果。
   *
   * @author AIGenerator
   * @param command 已由输入层转换的命令
   * @param current 已分配同步序列的当前账号
   * @param codeProof 已验证的登录验证码；密码登录为空
   * @param passwordSnapshot 密码验证使用的账号快照；验证码登录为空
   * @param passwordLogin 是否按密码快照复核
   * @return 当前动作的应用结果
   */
  private AuthAppResult issueSession(
      AuthCommand command,
      UserAccountEntity current,
      EmailChallengeEntity codeProof,
      UserAccountEntity passwordSnapshot,
      boolean passwordLogin) {
    // 1. 密码登录重新核对密码摘要和认证代次，阻断重置后的旧校验结果。
    if (passwordLogin
        && (!java.util.Objects.equals(current.passwordHash(), passwordSnapshot.passwordHash())
            || !current.authEpoch().equals(passwordSnapshot.authEpoch()))) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 2. 验证码登录在签发会话事务中消费证明，失败随账号创建和会话一起回滚。
    if (codeProof != null) {
      consume(codeProof);
    }
    // 3. 生成当前会话的随机凭据，持久层只保存不可逆摘要。
    String access = Ids.token(), refresh = Ids.token(), csrf = Ids.token(), sid = Ids.next();
    // 4. 取得浏览器设备标识的不可逆摘要，供本段后续处理使用。
    byte[] deviceHash = Ids.hash(command.deviceKey());
    var found =
        deviceRepository.query(
            QueryValue.all("id", 1)
                .where("user_id", "EQ", current.id())
                .where("device_key_hash", "EQ", deviceHash));
    DeviceEntity device;
    // 5. 本浏览器设备尚未登记时创建归属当前账号的设备实例。
    if (found.isEmpty()) {
      DeviceEntity created = DeviceAggregate.browser(current.id(), deviceHash, now()).entity();
      Transactions.require(
          ApplicationFailures.required(
                  authDomainService.saveDevice(
                      authDomainParamAssembler.device(new DeviceAggregate(created))))
              .saved());
      device =
          deviceRepository
              .query(QueryValue.all("id", 1).where("public_id", "EQ", created.publicId()))
              .get(0)
              .entity();
    } else {
      device = found.get(0).entity();
    }
    // 6. 读取当前浏览器设备的活动会话，准备按设备粒度替换。
    var active =
        loginSessionRepository.query(
            QueryValue.all("id", 1)
                .where("device_id", "EQ", device.id())
                .where("status", "EQ", "ACTIVE"));
    String revoked = null;
    // 7. 同浏览器设备已有活动会话时撤销旧会话，其他设备会话继续有效。
    if (!active.isEmpty()) {
      revoked = active.get(0).entity().publicId();
      Transactions.require(
          ApplicationFailures.required(
                  authDomainService.saveLoginSession(
                      authDomainParamAssembler.loginSession(
                          new LoginSessionAggregate(active.get(0).entity()).revoke("REPLACED"))))
              .saved());
    }
    // 8. 通过领域聚合语义准备当前设备的新会话。
    LoginSessionEntity created =
        LoginSessionAggregate.issue(
                current,
                device,
                sid,
                Ids.hash(access),
                Ids.hash(refresh),
                Ids.hash(csrf),
                now())
            .entity();
    // 9. 持久化新会话并与验证码消费、旧会话撤销保持同一事务。
    Transactions.require(
        ApplicationFailures.required(
                authDomainService.saveLoginSession(
                    authDomainParamAssembler.loginSession(new LoginSessionAggregate(created))))
            .saved());
    // 10. 记录有序同步事件和发件箱，供当前账号其他设备感知会话替换。
    events.append(current, "session.replaced", sid, 0, revoked, "{}");
    // 11. 读取持久化会话并将凭据与内部字段按协议隔离后返回。
    var stored =
        loginSessionRepository
            .query(QueryValue.all("id", 1).where("public_id", "EQ", sid))
            .get(0)
            .entity();
    return authAppAssembler.session(current, stored, access, refresh, csrf);
  }
}
