package com.hellotravel.application.auth.usecase;

import com.hellotravel.application.auth.assembler.AuthAppAssembler;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;
import com.hellotravel.application.auth.AuthWriteAppService;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.auth.adaptor.SecurityAdaptor;
import com.hellotravel.application.auth.assembler.SecurityCommandAppAssembler;
import com.hellotravel.application.auth.command.SecurityCommand;
import com.hellotravel.util.JsonUtil;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.RefreshReceiptAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.auth.repository.DeviceRepository;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.model.security.SecurityDO;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 邮箱认证、设备内替换、刷新与撤销编排；外部密码/频控在短事务之外。
 *
 * @author AIGenerator
 */
@Component
public final class AuthActionOperations {

    private final UserAccountRepository userAccountRepository;
    private final DeviceRepository deviceRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final EmailChallengeRepository emailChallengeRepository;
    private final RefreshReceiptRepository refreshReceiptRepository;
    private final AuthWriteAppService authWriteAppService;
    private final Transactions transactions;
    private final SyncEventPublisher events;
    private final SecurityAdaptor security;
    private final AuthAppAssembler authAppAssembler;
    private final SecurityCommandAppAssembler securityCommandAppAssembler;

    /**
     * 组装认证用例共享的仓储、领域写入、事务与安全协作。
     *
     * @param authWriteAppService 认证领域写入协作
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
    public AuthActionOperations(
            AuthWriteAppService authWriteAppService,
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
        this.authWriteAppService = authWriteAppService;
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

    private SecurityDO secure(SecurityCommand command) {
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        Result<SecurityDO> result = security.process(command);
        // 2. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return result.data();
    }

    private String email(String raw) {
        // 1. 拒绝为空、超长或不符合基本格式的邮箱，避免无效证明进入认证链路。
        if (raw == null || raw.length() > 254 || !raw.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 返回去空白、统一大小写的规范邮箱供唯一键与证明绑定使用。
        return raw.strip().toLowerCase(Locale.ROOT);
    }

    private void limit(AuthCommand command, String key) {
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        SecurityDO result = secure(securityCommandAppAssembler.limit(command.rateKey(), key));
        // 2. 核对安全端口返回的验证或限流结果，失败提前中止。
        if (!result.valid()) {
            throw new ApplicationException(ApplicationErrorCode.RATE_LIMITED);
        }
    }

    private UserAccountEntity account(String address) {
        // 1. 读取账号，按当前用例条件限定查询窗口。
        var list =
                userAccountRepository.query(
                        QueryValue.all("id", 1).where("email_normalized", "EQ", address));
        // 2. 返回规范邮箱对应的账号；记录不存在时交给认证步骤统一处理。
        return list.isEmpty() ? null : list.get(0).entity();
    }

    private EmailChallengeEntity proof(AuthCommand command, String purpose, String address) {
        // 1. 根据是否携带邮箱证明选择验证码或密码验证路径。
        if (command.challengeId() == null
                || command.code() == null
                || !command.code().matches("[0-9]{6}")) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 2. 读取邮箱验证码，按当前用例条件限定查询窗口。
        var found =
                emailChallengeRepository.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", command.challengeId()));
        // 3. 验证码记录缺失时统一拒绝邮箱证明。
        if (found.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
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
                                                        command.code(),
                                                        c.publicId()
                                                                + "|"
                                                                + address
                                                                + "|"
                                                                + purpose))
                                        .value());
        // 5. 核对验证码有效期、用途、状态与HMAC证明，验证失败统一拒绝。
        if (!usable || !MessageDigest.isEqual(computed, c.codeHmac())) {
            if (usable) {
                transactions.plain(
                        () -> {
                            // 1. 按可信内部标识读取邮箱验证码当前快照。
                            EmailChallengeEntity current =
                                    emailChallengeRepository.findById(c.id()).entity();
                            // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                            if ("ISSUED".equals(current.status())) {
                                Transactions.require(
                                        authWriteAppService.saveEmailChallenge(
                                                new EmailChallengeAggregate(current)
                                                        .failedAttempt()));
                            }
                            // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                            return null;
                        });
            }
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 6. 返回本段实际处理结果，保持本层输出契约。
        return c;
    }

    private void consume(EmailChallengeEntity proof) {
        // 1. 按可信内部标识读取邮箱验证码当前快照。
        EmailChallengeEntity current =
                emailChallengeRepository.findById(proof.id()).entity();
        // 2. 核对快照版本与预期版本，失败中止当前处理。
        if (!current.version().equals(proof.version()) || !"ISSUED".equals(current.status())) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
        Transactions.require(
                authWriteAppService.saveEmailChallenge(
                        new EmailChallengeAggregate(current).consume()));
    }

    private String hashPassword(String password) {
        // 1. 核对输入或读取结果的存在性，失败中止当前处理。
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 返回带算法、参数与盐的密码摘要，原始密码不进入数据库。
        return secure(securityCommandAppAssembler.hashPassword(password)).value();
    }

    /**
     * 签发指定用途的邮箱验证码。
     *
     * @param command 已校验的认证命令
     * @return 验证码公开标识
     * @author AIGenerator
     */
    public AuthAppResult challenge(AuthCommand command) {
        // 1. 规范化邮箱并限制输入长度。
        String address = email(command.email());
        // 2. 仅接受注册、登录和密码重置三种验证码用途。
        if (!Set.of("REGISTER", "LOGIN", "RESET_PASSWORD").contains(command.purpose())) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 3. 核对安全端口返回的验证或限流结果，失败提前中止。
        if (!secure(securityCommandAppAssembler.mailAvailable()).valid()) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
        // 4. 执行当前主体及用途的频控，超限提前中止。
        limit(command, "issue:" + address + ":" + command.purpose());
        // 5. 执行当前主体及用途的频控，超限提前中止。
        limit(command, "issue-ip");
        // 6. 生成本次业务的公开标识，内部数据库主键保持由仓储分配。
        String id = Ids.next();
        String code = secure(securityCommandAppAssembler.otp()).value();
        byte[] hmac =
                Base64.getDecoder()
                        .decode(
                                secure(
                                                securityCommandAppAssembler.hmac(
                                                        code,
                                                        id
                                                                + "|"
                                                                + address
                                                                + "|"
                                                                + command.purpose()))
                                        .value());
        byte[] encrypted =
                Base64.getDecoder()
                        .decode(secure(securityCommandAppAssembler.encrypt(code, id)).value());
        // 7. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.plain(
                () -> {
                    // 1. 读取邮箱验证码，按当前用例条件限定查询窗口。
                    var previous =
                            emailChallengeRepository.query(
                                    QueryValue.all("id", 10)
                                            .where("email_normalized", "EQ", address)
                                            .where("purpose", "EQ", command.purpose())
                                            .where(
                                                    "status",
                                                    "IN",
                                                    List.of("ISSUED", "PENDING_SEND")));
                    // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var old : previous) {
                        Transactions.require(
                                authWriteAppService.saveEmailChallenge(
                                        new EmailChallengeAggregate(old.entity())
                                                .delivery("REVOKED")));
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
                            authWriteAppService.saveEmailChallenge(
                                    new EmailChallengeAggregate(created)));
                    // 5. 同事务登记可恢复后台任务，外部调用在提交之后执行。
                    events.outbox(null, "EMAIL", id, JsonUtil.encode(Map.of("challengeId", id)));
                    // 6. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 8. 返回本段实际处理结果，保持本层输出契约。
        return authAppAssembler.challenge(id);
    }

    /**
     * 消费注册证明并创建账号。
     *
     * @param command 已校验的认证命令
     * @return 新账号公开视图
     * @author AIGenerator
     */
    public AuthAppResult register(AuthCommand command) {
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
                            authWriteAppService.saveUserAccount(new UserAccountAggregate(created)));
                    // 4. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 5. 取得新账号的持久化快照，供本段后续处理使用。
        UserAccountEntity user = account(address);
        // 6. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
        return authAppAssembler.account(user);
    }

    /**
     * 校验密码或登录证明并签发设备会话。
     *
     * @param command 已校验的认证命令
     * @return 当前会话与公开账号视图
     * @author AIGenerator
     */
    public AuthAppResult login(AuthCommand command) {
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
            boolean valid =
                    secure(securityCommandAppAssembler.passwordProof(command, snapshot)).valid();
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
                        Transactions.require(authWriteAppService.saveDevice(new DeviceAggregate(created)));
                        device =
                                deviceRepository
                                        .query(
                                                QueryValue.all("id", 1)
                                                        .where(
                                                                "public_id",
                                                                "EQ",
                                                                created.publicId()))
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
                                authWriteAppService.saveLoginSession(
                                        new LoginSessionAggregate(active.get(0).entity())
                                                .revoke("REPLACED")));
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
                            authWriteAppService.saveLoginSession(new LoginSessionAggregate(created)));
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

    private LoginSessionEntity session(String token, boolean refresh) {
        // 1. 核对输入或读取结果的存在性，失败中止当前处理。
        if (token == null || token.length() > 200) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 2. 读取登录会话，按当前用例条件限定查询窗口。
        var found =
                loginSessionRepository.query(
                        QueryValue.all("id", 1)
                                .where(
                                        refresh ? "refresh_token_hash" : "access_token_hash",
                                        "EQ",
                                        Ids.hash(token)));
        // 3. 凭据没有匹配会话时返回认证失效，不猜测历史会话。
        if (found.isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 4. 返回凭据摘要对应的登录快照，活动状态由下一步骤核验。
        return found.get(0).entity();
    }

    private UserAccountEntity valid(LoginSessionEntity session, String expected, boolean refresh) {
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
                || !(refresh ? session.refreshExpiresAt() : session.accessExpiresAt())
                        .isAfter(now())) {
            throw new ApplicationException(ApplicationErrorCode.UNAUTHORIZED);
        }
        // 4. 返回通过会话、页面SID、有效期与认证代次核验的账号。
        return user;
    }

    /**
     * 校验页面SID和访问令牌对应的活动会话。
     *
     * @param command 已校验的认证命令
     * @return 已验证会话主体
     * @author AIGenerator
     */
    public AuthAppResult check(AuthCommand command) {
        // 1. 取得当前登录会话快照，供本段后续处理使用。
        LoginSessionEntity s = session(command.accessToken(), false);
        // 2. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
        return authAppAssembler.checked(valid(s, command.expectedSid(), false), s);
    }

    /**
     * 消费刷新令牌并轮换会话凭据。
     *
     * @param command 已校验的认证命令
     * @return 轮换后的会话主体
     * @author AIGenerator
     */
    public AuthAppResult refresh(AuthCommand command) {
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
                            loginSessionRepository
                                    .findById(receipts.get(0).entity().sessionId())
                                    .entity();
                    if (!prior.publicId().equals(command.expectedSid())) {
                        throw new ApplicationException(ApplicationErrorCode.SESSION_REPLACED);
                    }
                    transactions.mutate(
                            prior.userId(),
                            owner -> {
                                // 1. 按可信内部标识读取登录会话当前快照。
                                var current =
                                        loginSessionRepository.findById(prior.id()).entity();
                                // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                                Transactions.require(
                                        authWriteAppService.saveLoginSession(
                                                new LoginSessionAggregate(current)
                                                        .revoke("REFRESH_REPLAY")));
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
                    LoginSessionEntity stored =
                            loginSessionRepository.findById(snapshot.id()).entity();
                    // 2. 执行valid职责步骤，并把失败交给所属事务或入口处理。
                    valid(stored, command.expectedSid(), true);
                    // 3. 刷新证明与已加载快照不一致时拒绝轮换，阻断消费后的重放。
                    if (!MessageDigest.isEqual(
                            stored.refreshTokenHash(), Ids.hash(command.refreshToken()))) {
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
                            authWriteAppService.saveRefreshReceipt(new RefreshReceiptAggregate(receipt)));
                    // 6. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                    LoginSessionEntity rotated =
                            new LoginSessionAggregate(stored)
                                    .rotate(
                                            Ids.hash(access),
                                            Ids.hash(refresh),
                                            Ids.hash(csrf),
                                            now().plusMinutes(15))
                                    .entity();
                    // 7. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            authWriteAppService.saveLoginSession(new LoginSessionAggregate(rotated)));
                    // 8. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                    events.append(
                            current,
                            "session.refreshed",
                            stored.publicId(),
                            stored.version() + 1,
                            null,
                            "{}");
                    // 9. 将已读取快照转为本用例视图，凭据与内部字段按协议隔离。
                    return authAppAssembler.session(
                            current, rotated, access, refresh, csrf);
                });
    }

    /**
     * 撤销当前页面绑定的会话。
     *
     * @param command 已校验的认证命令
     * @return 已撤销会话的主体视图
     * @author AIGenerator
     */
    public AuthAppResult logout(AuthCommand command) {
        // 1. 取得当前请求的认证主体，供本段后续处理使用。
        AuthAppResult principal = check(command);
        // 2. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.mutate(
                principal.userId(),
                current -> {
                    // 1. 按可信内部标识读取登录会话当前快照。
                    LoginSessionEntity stored =
                            loginSessionRepository.findById(principal.sessionId()).entity();
                    // 2. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            authWriteAppService.saveLoginSession(
                                    new LoginSessionAggregate(stored).revoke("LOGOUT")));
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

    /**
     * 消费重置证明、更新密码并撤销旧会话。
     *
     * @param command 已校验的认证命令
     * @return 已更新账号视图
     * @author AIGenerator
     */
    public AuthAppResult reset(AuthCommand command) {
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
                    UserAccountEntity locked =
                            userAccountRepository.findById(current.id()).entity();
                    // 3. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                    Transactions.require(
                            authWriteAppService.saveUserAccount(
                                    new UserAccountAggregate(locked).resetPassword(hash)));
                    // 4. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var active :
                            loginSessionRepository.query(
                                    QueryValue.all("id", 1000)
                                            .where("user_id", "EQ", current.id())
                                            .where("status", "EQ", "ACTIVE"))) {
                        Transactions.require(
                                authWriteAppService.saveLoginSession(
                                        new LoginSessionAggregate(active.entity())
                                                .revoke("PASSWORD_RESET")));
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

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
