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
    // 3. 保留当前来源或状态快照，后续核对并发变更与重复执行。
    UserAccountEntity snapshot = account(address);
    EmailChallengeEntity verified = null;
    // 4. 根据是否携带邮箱证明选择验证码或密码验证路径。
    if (command.challengeId() != null && !command.challengeId().isBlank()) {
      verified = proof(command, "LOGIN", address);
    } else {
      boolean valid = secure(securityCommandAppAssembler.passwordProof(command, snapshot)).valid();
      if (!valid) {
        throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
      }
    }
    // 5. 本浏览器设备尚未登记时创建归属当前账号的设备实例。
    if (snapshot == null || !"ACTIVE".equals(snapshot.status())) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 6. 固定外部校验结果，事务内再次核对账号代次。
    EmailChallengeEntity codeProof = verified;
    String access = Ids.token(), refresh = Ids.token(), csrf = Ids.token(), sid = Ids.next();
    // 7. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
    return transactions.mutate(
        snapshot.id(),
        current -> {
          // 1. 再次核对密码快照与账号认证代次，阻断重置后的旧校验结果。
          if (!current.passwordHash().equals(snapshot.passwordHash())
              || !current.authEpoch().equals(snapshot.authEpoch())) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
          }
          // 2. 验证码登录在签发会话事务中消费证明，失败随业务一起回滚。
          if (codeProof != null) {
            consume(codeProof);
          }
          // 3. 取得浏览器设备标识的不可逆摘要，供本段后续处理使用。
          byte[] deviceHash = Ids.hash(command.deviceKey());
          var found =
              deviceRepository.query(
                  QueryValue.all("id", 1)
                      .where("user_id", "EQ", current.id())
                      .where("device_key_hash", "EQ", deviceHash));
          DeviceEntity device;
          // 4. 本浏览器设备尚未登记时创建归属当前账号的设备实例。
          if (found.isEmpty()) {
            DeviceEntity created =
                DeviceAggregate.browser(current.id(), deviceHash, now()).entity();
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
          // 5. 读取登录会话，按当前用例条件限定查询窗口。
          var active =
              loginSessionRepository.query(
                  QueryValue.all("id", 1)
                      .where("device_id", "EQ", device.id())
                      .where("status", "EQ", "ACTIVE"));
          String revoked = null;
          // 6. 同浏览器设备已有活动会话时撤销旧会话，其他设备会话继续有效。
          if (!active.isEmpty()) {
            revoked = active.get(0).entity().publicId();
            Transactions.require(
                ApplicationFailures.required(
                        authDomainService.saveLoginSession(
                            authDomainParamAssembler.loginSession(
                                new LoginSessionAggregate(active.get(0).entity())
                                    .revoke("REPLACED"))))
                    .saved());
          }
          // 7. 通过领域聚合语义准备业务快照，固定状态由实体封装。
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
          // 8. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          Transactions.require(
              ApplicationFailures.required(
                      authDomainService.saveLoginSession(
                          authDomainParamAssembler.loginSession(
                              new LoginSessionAggregate(created))))
                  .saved());
          // 9. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
          events.append(current, "session.replaced", sid, 0, revoked, "{}");
          // 10. 读取登录会话，按当前用例条件限定查询窗口。
          var stored =
              loginSessionRepository
                  .query(QueryValue.all("id", 1).where("public_id", "EQ", sid))
                  .get(0)
                  .entity();
          // 11. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
          return authAppAssembler.session(current, stored, access, refresh, csrf);
        });
  }
}
