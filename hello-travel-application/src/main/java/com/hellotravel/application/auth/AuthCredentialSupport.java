package com.hellotravel.application.auth;

import com.hellotravel.application.auth.adaptor.SecurityAdaptor;
import com.hellotravel.application.auth.assembler.AuthAppAssembler;
import com.hellotravel.application.auth.assembler.AuthDomainParamAssembler;
import com.hellotravel.application.auth.assembler.SecurityCommandAppAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.command.SecurityCommand;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
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
import com.hellotravel.model.security.SecurityDO;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;

/**
 * AuthCredentialSupport提供跨认证动作复用的受控校验与访问能力，不承载动作编排。
 *
 * @author AIGenerator
 */
abstract class AuthCredentialSupport {

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final UserAccountRepository userAccountRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final DeviceRepository deviceRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final LoginSessionRepository loginSessionRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final EmailChallengeRepository emailChallengeRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final RefreshReceiptRepository refreshReceiptRepository;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final AuthDomainService authDomainService;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final AuthDomainParamAssembler authDomainParamAssembler;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final Transactions transactions;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final SyncEventPublisher events;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final SecurityAdaptor security;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final AuthAppAssembler authAppAssembler;

  /**
   * 跨动作复用的受控依赖。
   *
   * @author AIGenerator
   */
  protected final SecurityCommandAppAssembler securityCommandAppAssembler;

  /**
   * 组装认证用例共享的仓储、领域写入、事务与安全协作。
   *
   * @param authDomainService 认证领域规则入口
   * @param authDomainParamAssembler 认证聚合到领域参数的转换器
   * @param userAccountRepository 账号仓储
   * @param deviceRepository 设备仓储
   * @param loginSessionRepository 登录会话仓储
   * @param emailChallengeRepository 邮箱验证仓储
   * @param refreshReceiptRepository 刷新消费凭据仓储
   * @param transactions 短事务协作
   * @param events 持久同步事件协作
   * @param security 安全能力端口
   * @param authAppAssembler 认证结果转换器
   * @param securityCommandAppAssembler 安全命令转换器
   * @author AIGenerator
   */
  protected AuthCredentialSupport(
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
    this.authDomainService = authDomainService;
    this.authDomainParamAssembler = authDomainParamAssembler;
    this.userAccountRepository = userAccountRepository;
    this.deviceRepository = deviceRepository;
    this.loginSessionRepository = loginSessionRepository;
    this.emailChallengeRepository = emailChallengeRepository;
    this.refreshReceiptRepository = refreshReceiptRepository;
    this.transactions = transactions;
    this.events = events;
    this.security = security;
    this.authAppAssembler = authAppAssembler;
    this.securityCommandAppAssembler = securityCommandAppAssembler;
  }

  protected SecurityDO secure(SecurityCommand command) {
    // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
    Result<SecurityDO> result = security.process(command);
    // 2. 核对下层标准结果的成功状态，失败中止当前处理。
    if (!result.success()) {
      throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
    }
    // 3. 返回本段实际处理结果，保持本层输出契约。
    return result.data();
  }

  protected String email(String raw) {
    // 1. 拒绝为空、超长或不符合基本格式的邮箱，避免无效证明进入认证链路。
    if (raw == null || raw.length() > 254 || !raw.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new ApplicationException(ApplicationErrorCode.INVALID);
    }
    // 2. 返回去空白、统一大小写的规范邮箱供唯一键与证明绑定使用。
    return raw.strip().toLowerCase(Locale.ROOT);
  }

  protected void limit(AuthCommand command, String key) {
    // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
    SecurityDO result = secure(securityCommandAppAssembler.limit(command.rateKey(), key));
    // 2. 核对安全端口返回的验证或限流结果，失败提前中止。
    if (!result.valid()) {
      throw new ApplicationException(ApplicationErrorCode.RATE_LIMITED);
    }
  }

  protected UserAccountEntity account(String address) {
    // 1. 读取账号，按当前用例条件限定查询窗口。
    var list =
        userAccountRepository.query(
            QueryValue.all("id", 1).where("email_normalized", "EQ", address));
    // 2. 返回规范邮箱对应的账号；记录不存在时交给认证步骤统一处理。
    return list.isEmpty() ? null : list.get(0).entity();
  }

  protected EmailChallengeEntity proof(AuthCommand command, String purpose, String address) {
    // 1. 根据是否携带邮箱证明选择验证码或密码验证路径。
    if (command.challengeId() == null
        || command.code() == null
        || !command.code().matches("[0-9]{6}")) {
      throw new ApplicationException(ApplicationErrorCode.EMAIL_CHALLENGE_UNAVAILABLE);
    }
    // 2. 读取邮箱验证码，按当前用例条件限定查询窗口。
    var found =
        emailChallengeRepository.query(
            QueryValue.all("id", 1).where("public_id", "EQ", command.challengeId()));
    // 3. 验证码记录缺失时统一拒绝邮箱证明。
    if (found.isEmpty()) {
      throw new ApplicationException(ApplicationErrorCode.EMAIL_CHALLENGE_UNAVAILABLE);
    }
    // 4. 取得当前对话或验证码快照，供本段后续处理使用。
    EmailChallengeEntity c = found.get(0).entity();
    boolean usable =
        address.equals(c.emailNormalized())
            && purpose.equals(c.purpose())
            && "ISSUED".equals(c.status())
            && c.attempts() < c.maxAttempts()
            && c.expiresAt().isAfter(now());
    byte[] computed =
        Base64.getDecoder()
            .decode(
                secure(
                        securityCommandAppAssembler.hmac(
                            command.code(), c.publicId() + "|" + address + "|" + purpose))
                    .value());
    // 5. 核对验证码有效期、用途、状态与HMAC证明，验证失败统一拒绝。
    if (!usable || !MessageDigest.isEqual(computed, c.codeHmac())) {
      if (usable) {
        transactions.plain(
            () -> {
              // 1. 按可信内部标识读取邮箱验证码当前快照。
              EmailChallengeEntity current = emailChallengeRepository.findById(c.id()).entity();
              // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
              if ("ISSUED".equals(current.status())) {
                Transactions.require(
                    ApplicationFailures.required(
                            authDomainService.saveEmailChallenge(
                                authDomainParamAssembler.emailChallenge(
                                    new EmailChallengeAggregate(current).failedAttempt())))
                        .saved());
              }
              // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
              return null;
            });
      }
      throw new ApplicationException(ApplicationErrorCode.EMAIL_CHALLENGE_UNAVAILABLE);
    }
    // 6. 返回本段实际处理结果，保持本层输出契约。
    return c;
  }

  protected void consume(EmailChallengeEntity proof) {
    // 1. 按可信内部标识读取邮箱验证码当前快照。
    EmailChallengeEntity current = emailChallengeRepository.findById(proof.id()).entity();
    // 2. 核对快照版本与预期版本，失败中止当前处理。
    if (!current.version().equals(proof.version()) || !"ISSUED".equals(current.status())) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
    Transactions.require(
        ApplicationFailures.required(
                authDomainService.saveEmailChallenge(
                    authDomainParamAssembler.emailChallenge(
                        new EmailChallengeAggregate(current).consume())))
            .saved());
  }

  protected String hashPassword(String password) {
    // 1. 核对输入或读取结果的存在性，失败中止当前处理。
    if (password == null || password.length() < 12 || password.length() > 128) {
      throw new ApplicationException(ApplicationErrorCode.INVALID);
    }
    // 2. 返回带算法、参数与盐的密码摘要，原始密码不进入数据库。
    return secure(securityCommandAppAssembler.hashPassword(password)).value();
  }

  protected LoginSessionEntity session(String token, boolean refresh) {
    // 1. 核对输入或读取结果的存在性，失败中止当前处理。
    if (token == null || token.length() > 200) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 2. 读取登录会话，按当前用例条件限定查询窗口。
    var found =
        loginSessionRepository.query(
            QueryValue.all("id", 1)
                .where(
                    refresh ? "refresh_token_hash" : "access_token_hash", "EQ", Ids.hash(token)));
    // 3. 凭据没有匹配会话时返回认证失效，不猜测历史会话。
    if (found.isEmpty()) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 4. 返回凭据摘要对应的登录快照，活动状态由下一步骤核验。
    return found.get(0).entity();
  }

  protected UserAccountEntity valid(LoginSessionEntity session, String expected, boolean refresh) {
    // 1. 核对页面绑定的会话SID，旧页面不得借新Cookie恢复为有效身份。
    if (!session.publicId().equals(expected)) {
      throw new ApplicationException(ApplicationErrorCode.SESSION_REPLACED);
    }
    // 2. 按可信内部标识读取账号当前快照。
    UserAccountEntity user = userAccountRepository.findById(session.userId()).entity();
    // 3. 再次核对密码快照与账号认证代次，阻断重置后的旧校验结果。
    if (!"ACTIVE".equals(session.status())
        || !"ACTIVE".equals(user.status())
        || !session.authEpoch().equals(user.authEpoch())
        || !(refresh ? session.refreshExpiresAt() : session.accessExpiresAt()).isAfter(now())) {
      throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
    }
    // 4. 返回通过会话、页面SID、有效期与认证代次核验的账号。
    return user;
  }

  /**
   * 校验当前访问令牌对应的活动会话，并组装认证主体。
   *
   * @param command 已完成输入校验的认证命令
   * @return 可供后续动作复用的认证主体
   * @author AIGenerator
   */
  protected AuthAppResult authenticated(AuthCommand command) {
    // 1. 读取访问令牌绑定的会话快照。
    LoginSessionEntity session = session(command.accessToken(), false);
    // 2. 校验会话、账号、页面 SID 与认证代次。
    UserAccountEntity user = valid(session, command.expectedSid(), false);
    // 3. 组装动作可复用的认证主体，避免动作服务相互调用。
    return authAppAssembler.checked(user, session);
  }

  protected static LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
