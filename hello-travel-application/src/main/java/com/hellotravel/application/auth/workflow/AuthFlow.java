package com.hellotravel.application.auth.workflow;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthResult;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.command.SecurityCommand;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.auth.model.aggregate.DeviceAggregate;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;
import com.hellotravel.domain.auth.model.entity.DeviceEntity;
import com.hellotravel.domain.auth.model.entity.EmailChallengeEntity;
import com.hellotravel.domain.auth.model.entity.LoginSessionEntity;
import com.hellotravel.domain.auth.model.entity.UserAccountEntity;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
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
public final class AuthFlow {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    private final SecurityOutAdaptor security;

    public AuthFlow(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            SyncEvents events,
            SecurityOutAdaptor security) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.events = events;
        this.security = security;
    }

    /**
     * 分发可信业务动作。
     *
     * @author AIGenerator
     * @param command 受控command参数
     * @return 当前操作的业务结果
     */
    public AuthResult perform(AuthCommand command) {
        return switch (command.action()) {
            case "CHALLENGE" -> challenge(command);
            case "REGISTER" -> register(command);
            case "LOGIN" -> login(command);
            case "REFRESH" -> refresh(command);
            case "CHECK" -> check(command);
            case "RESET" -> reset(command);
            case "LOGOUT" -> logout(command);
            default -> throw new DomainException(DomainErrorCode.INVALID);
        };
    }

    private SecurityDO secure(String action, String value, String proof, String scope) {
        Result<SecurityDO> result =
                security.process(new SecurityCommand(action, value, proof, scope));
        if (!result.success()) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
        return result.data();
    }

    private String email(String raw) {
        if (raw == null || raw.length() > 254 || !raw.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        return raw.strip().toLowerCase(Locale.ROOT);
    }

    private void limit(AuthCommand command, String key) {
        SecurityDO result = secure("LIMIT", command.rateKey(), null, key);
        if (!result.valid()) {
            throw new DomainException(DomainErrorCode.RATE_LIMITED);
        }
    }

    private UserAccountEntity account(String address) {
        var list =
                repositories.userAccount.query(
                        QueryValue.all("id", 1).where("email_normalized", "EQ", address));
        return list.isEmpty() ? null : list.get(0).entity();
    }

    private EmailChallengeEntity proof(AuthCommand command, String purpose, String address) {
        if (command.challengeId() == null
                || command.code() == null
                || !command.code().matches("[0-9]{6}")) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        var found =
                repositories.emailChallenge.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", command.challengeId()));
        if (found.isEmpty()) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
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
                                                "HMAC",
                                                command.code(),
                                                null,
                                                c.publicId() + "|" + address + "|" + purpose)
                                        .value());
        if (!usable || !MessageDigest.isEqual(computed, c.codeHmac())) {
            if (usable) {
                transactions.plain(
                        () -> {
                            EmailChallengeEntity current =
                                    repositories.emailChallenge.findById(c.id()).entity();
                            if ("ISSUED".equals(current.status())) {
                                Transactions.require(
                                        writes.saveEmailChallenge(
                                                new EmailChallengeAggregate(
                                                        current.failedAttempt())));
                            }
                            return null;
                        });
            }
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        return c;
    }

    private void consume(EmailChallengeEntity proof) {
        EmailChallengeEntity current = repositories.emailChallenge.findById(proof.id()).entity();
        if (!current.version().equals(proof.version()) || !"ISSUED".equals(current.status())) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        Transactions.require(
                writes.saveEmailChallenge(new EmailChallengeAggregate(current.consume())));
    }

    private String hashPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        return secure("HASH_PASSWORD", password, null, null).value();
    }

    private AuthResult challenge(AuthCommand command) {
        String address = email(command.email());
        if (!Set.of("REGISTER", "LOGIN", "RESET_PASSWORD").contains(command.purpose())) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        if (!secure("MAIL_AVAILABLE", null, null, null).valid()) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
        limit(command, "issue:" + address + ":" + command.purpose());
        limit(command, "issue-ip");
        String id = Ids.next();
        String code = secure("OTP", null, null, null).value();
        byte[] hmac =
                Base64.getDecoder()
                        .decode(
                                secure(
                                                "HMAC",
                                                code,
                                                null,
                                                id + "|" + address + "|" + command.purpose())
                                        .value());
        byte[] encrypted = Base64.getDecoder().decode(secure("ENCRYPT", code, null, id).value());
        transactions.plain(
                () -> {
                    var previous =
                            repositories.emailChallenge.query(
                                    QueryValue.all("id", 10)
                                            .where("email_normalized", "EQ", address)
                                            .where("purpose", "EQ", command.purpose())
                                            .where(
                                                    "status",
                                                    "IN",
                                                    List.of("ISSUED", "PENDING_SEND")));
                    for (var old : previous) {
                        Transactions.require(
                                writes.saveEmailChallenge(
                                        new EmailChallengeAggregate(
                                                old.entity().delivery("REVOKED"))));
                    }
                    EmailChallengeEntity created =
                            new EmailChallengeEntity(
                                    null,
                                    id,
                                    address,
                                    command.purpose(),
                                    hmac,
                                    "v1",
                                    encrypted,
                                    "v1",
                                    "PENDING_SEND",
                                    0,
                                    5,
                                    now().plusMinutes(5),
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(
                            writes.saveEmailChallenge(new EmailChallengeAggregate(created)));
                    events.outbox(null, "EMAIL", id, Json.encode(Map.of("challengeId", id)));
                    return null;
                });
        return new AuthResult(null, null, null, null, null, null, null, null, id);
    }

    private AuthResult register(AuthCommand command) {
        String address = email(command.email());
        limit(command, "register:" + address);
        EmailChallengeEntity verified = proof(command, "REGISTER", address);
        String hash = hashPassword(command.password());
        transactions.plain(
                () -> {
                    consume(verified);
                    UserAccountEntity created =
                            new UserAccountEntity(
                                    null,
                                    com.hellotravel.common.identity.Ids.next(),
                                    address,
                                    hash,
                                    now(),
                                    "ACTIVE",
                                    0L,
                                    0L,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(writes.saveUserAccount(new UserAccountAggregate(created)));
                    return null;
                });
        UserAccountEntity user = account(address);
        return view(user, null, null, null, null);
    }

    private AuthResult login(AuthCommand command) {
        String address = email(command.email());
        limit(command, "login:" + address);
        UserAccountEntity snapshot = account(address);
        EmailChallengeEntity verified = null;
        if (command.challengeId() != null && !command.challengeId().isBlank()) {
            verified = proof(command, "LOGIN", address);
        } else {
            String fake =
                    "$argon2id$v=19$m=19456,t=2,p=1$MDEyMzQ1Njc4OWFiY2RlZg$U"
                            + "Vh1B85rlNqd2WCQ9z89uC0mPPsTLcI90J6V3fZnMmM";
            boolean valid =
                    secure(
                                    "VERIFY_PASSWORD",
                                    command.password() == null ? "" : command.password(),
                                    snapshot == null ? fake : snapshot.passwordHash(),
                                    null)
                            .valid();
            if (!valid) {
                throw new DomainException(DomainErrorCode.UNAUTHORIZED);
            }
        }
        if (snapshot == null || !"ACTIVE".equals(snapshot.status())) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        EmailChallengeEntity codeProof = verified;
        String access = Ids.token(), refresh = Ids.token(), csrf = Ids.token(), sid = Ids.next();
        return transactions.mutate(
                snapshot.id(),
                current -> {
                    if (!current.passwordHash().equals(snapshot.passwordHash())
                            || !current.authEpoch().equals(snapshot.authEpoch())) {
                        throw new DomainException(DomainErrorCode.UNAUTHORIZED);
                    }
                    if (codeProof != null) {
                        consume(codeProof);
                    }
                    byte[] deviceHash = Ids.hash(command.deviceKey());
                    var found =
                            repositories.device.query(
                                    QueryValue.all("id", 1)
                                            .where("user_id", "EQ", current.id())
                                            .where("device_key_hash", "EQ", deviceHash));
                    DeviceEntity device;
                    if (found.isEmpty()) {
                        DeviceEntity created =
                                new DeviceEntity(
                                        null,
                                        com.hellotravel.common.identity.Ids.next(),
                                        current.id(),
                                        deviceHash,
                                        "浏览器",
                                        now(),
                                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                        java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                        0L);
                        Transactions.require(writes.saveDevice(new DeviceAggregate(created)));
                        device =
                                repositories
                                        .device
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
                    var active =
                            repositories.loginSession.query(
                                    QueryValue.all("id", 1)
                                            .where("device_id", "EQ", device.id())
                                            .where("status", "EQ", "ACTIVE"));
                    String revoked = null;
                    if (!active.isEmpty()) {
                        revoked = active.get(0).entity().publicId();
                        Transactions.require(
                                writes.saveLoginSession(
                                        new LoginSessionAggregate(
                                                active.get(0).entity().revoke("REPLACED"))));
                    }
                    LoginSessionEntity created =
                            new LoginSessionEntity(
                                    null,
                                    sid,
                                    current.id(),
                                    device.id(),
                                    Ids.hash(access),
                                    Ids.hash(refresh),
                                    Ids.hash(csrf),
                                    current.authEpoch(),
                                    "ACTIVE",
                                    null,
                                    now().plusMinutes(15),
                                    now().plusDays(7),
                                    now(),
                                    null,
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                    0L);
                    Transactions.require(
                            writes.saveLoginSession(new LoginSessionAggregate(created)));
                    events.append(current, "session.replaced", sid, 0, revoked, "{}");
                    var stored =
                            repositories
                                    .loginSession
                                    .query(QueryValue.all("id", 1).where("public_id", "EQ", sid))
                                    .get(0)
                                    .entity();
                    return view(current, stored, access, refresh, csrf);
                });
    }

    private LoginSessionEntity session(String token, boolean refresh) {
        if (token == null || token.length() > 200) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        var found =
                repositories.loginSession.query(
                        QueryValue.all("id", 1)
                                .where(
                                        refresh ? "refresh_token_hash" : "access_token_hash",
                                        "EQ",
                                        Ids.hash(token)));
        if (found.isEmpty()) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        return found.get(0).entity();
    }

    private UserAccountEntity valid(LoginSessionEntity session, String expected, boolean refresh) {
        if (!session.publicId().equals(expected)) {
            throw new DomainException(DomainErrorCode.SESSION_REPLACED);
        }
        UserAccountEntity user = repositories.userAccount.findById(session.userId()).entity();
        if (!"ACTIVE".equals(session.status())
                || !"ACTIVE".equals(user.status())
                || !session.authEpoch().equals(user.authEpoch())
                || !(refresh ? session.refreshExpiresAt() : session.accessExpiresAt())
                        .isAfter(now())) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private AuthResult check(AuthCommand command) {
        LoginSessionEntity s = session(command.accessToken(), false);
        return view(valid(s, command.expectedSid(), false), s, null, null, null);
    }

    private AuthResult refresh(AuthCommand command) {
        LoginSessionEntity snapshot;
        try {
            snapshot = session(command.refreshToken(), true);
        } catch (DomainException exception) {
            if (command.refreshToken() != null && command.refreshToken().length() <= 200) {
                var receipts =
                        repositories.refreshReceipt.query(
                                QueryValue.all("id", 1)
                                        .where("token_hash", "EQ", Ids.hash(command.refreshToken()))
                                        .where("expires_at", "GT", now()));
                if (!receipts.isEmpty()) {
                    var prior =
                            repositories
                                    .loginSession
                                    .findById(receipts.get(0).entity().sessionId())
                                    .entity();
                    if (!prior.publicId().equals(command.expectedSid())) {
                        throw new DomainException(DomainErrorCode.SESSION_REPLACED);
                    }
                    transactions.mutate(
                            prior.userId(),
                            owner -> {
                                var current =
                                        repositories.loginSession.findById(prior.id()).entity();
                                Transactions.require(
                                        writes.saveLoginSession(
                                                new LoginSessionAggregate(
                                                        current.revoke("REFRESH_REPLAY"))));
                                events.append(
                                        owner,
                                        "session.revoked",
                                        current.publicId(),
                                        current.version() + 1,
                                        current.publicId(),
                                        "{}");
                                return null;
                            });
                }
            }
            throw exception;
        }
        UserAccountEntity owner = valid(snapshot, command.expectedSid(), true);
        if (command.csrf() == null
                || !MessageDigest.isEqual(snapshot.csrfTokenHash(), Ids.hash(command.csrf()))) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        String access = Ids.token(), refresh = Ids.token(), csrf = Ids.token();
        return transactions.mutate(
                owner.id(),
                current -> {
                    LoginSessionEntity stored =
                            repositories.loginSession.findById(snapshot.id()).entity();
                    valid(stored, command.expectedSid(), true);
                    if (!MessageDigest.isEqual(
                            stored.refreshTokenHash(), Ids.hash(command.refreshToken()))) {
                        throw new DomainException(DomainErrorCode.UNAUTHORIZED);
                    }
                    var receipt =
                            new com.hellotravel.domain.auth.model.entity.RefreshReceiptEntity(
                                    null,
                                    stored.userId(),
                                    stored.id(),
                                    Ids.hash(command.refreshToken()),
                                    now(),
                                    stored.refreshExpiresAt());
                    Transactions.require(
                            writes.saveRefreshReceipt(
                                    new com.hellotravel.domain.auth.model.aggregate
                                            .RefreshReceiptAggregate(receipt)));
                    LoginSessionEntity rotated =
                            stored.rotate(
                                    Ids.hash(access),
                                    Ids.hash(refresh),
                                    Ids.hash(csrf),
                                    now().plusMinutes(15));
                    Transactions.require(
                            writes.saveLoginSession(new LoginSessionAggregate(rotated)));
                    events.append(
                            current,
                            "session.refreshed",
                            stored.publicId(),
                            stored.version() + 1,
                            null,
                            "{}");
                    return view(current, rotated, access, refresh, csrf);
                });
    }

    private AuthResult logout(AuthCommand command) {
        AuthResult principal = check(command);
        transactions.mutate(
                principal.userId(),
                current -> {
                    LoginSessionEntity stored =
                            repositories.loginSession.findById(principal.sessionId()).entity();
                    Transactions.require(
                            writes.saveLoginSession(
                                    new LoginSessionAggregate(stored.revoke("LOGOUT"))));
                    events.append(
                            current,
                            "session.revoked",
                            stored.publicId(),
                            stored.version() + 1,
                            stored.publicId(),
                            "{}");
                    return null;
                });
        return principal;
    }

    private AuthResult reset(AuthCommand command) {
        String address = email(command.email());
        limit(command, "reset:" + address);
        EmailChallengeEntity verified = proof(command, "RESET_PASSWORD", address);
        String hash = hashPassword(command.password());
        UserAccountEntity owner = account(address);
        if (owner == null) {
            throw new DomainException(DomainErrorCode.UNAUTHORIZED);
        }
        transactions.mutate(
                owner.id(),
                current -> {
                    consume(verified);
                    UserAccountEntity locked =
                            repositories.userAccount.findById(current.id()).entity();
                    Transactions.require(
                            writes.saveUserAccount(
                                    new UserAccountAggregate(locked.resetPassword(hash))));
                    for (var active :
                            repositories.loginSession.query(
                                    QueryValue.all("id", 1000)
                                            .where("user_id", "EQ", current.id())
                                            .where("status", "EQ", "ACTIVE"))) {
                        Transactions.require(
                                writes.saveLoginSession(
                                        new LoginSessionAggregate(
                                                active.entity().revoke("PASSWORD_RESET"))));
                    }
                    events.append(
                            current,
                            "account.sessions_revoked",
                            owner.publicId(),
                            locked.version() + 1,
                            null,
                            "{}");
                    return null;
                });
        return view(owner, null, null, null, null);
    }

    private AuthResult view(
            UserAccountEntity account,
            LoginSessionEntity session,
            String access,
            String refresh,
            String csrf) {
        return new AuthResult(
                account.id(),
                account.publicId(),
                account.emailNormalized(),
                session == null ? null : session.publicId(),
                session == null ? null : session.id(),
                access,
                refresh,
                csrf,
                null);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
