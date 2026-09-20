package com.hellotravel.application.jobs.job;

import com.hellotravel.application.auth.AuthWriteAppService;
import com.hellotravel.application.chat.support.SyncWrites;
import com.hellotravel.application.chat.support.SyncEventPublisher;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.auth.repository.EmailChallengeRepository;
import com.hellotravel.domain.auth.repository.LoginSessionRepository;
import com.hellotravel.domain.auth.repository.RefreshReceiptRepository;
import com.hellotravel.domain.auth.repository.UserAccountRepository;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.domain.sync.repository.SyncEventRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 分批清理过期同步事件和刷新消费证据，保留期内的补齐与安全判定不变。
 *
 * @author AIGenerator
 */
@Component
public final class RetentionJob {

    private final UserAccountRepository userAccountRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final EmailChallengeRepository emailChallengeRepository;
    private final RefreshReceiptRepository refreshReceiptRepository;
    private final SyncEventRepository syncEventRepository;
    private final AuthWriteAppService authWriteAppService;
    private final SyncWrites syncWrites;
    private final Transactions transactions;
    private final SyncEventPublisher events;

    public RetentionJob(
            UserAccountRepository userAccountRepository,
            LoginSessionRepository loginSessionRepository,
            EmailChallengeRepository emailChallengeRepository,
            RefreshReceiptRepository refreshReceiptRepository,
            SyncEventRepository syncEventRepository,
            AuthWriteAppService authWriteAppService,
            SyncWrites syncWrites,
            Transactions transactions,
            SyncEventPublisher events) {
        this.userAccountRepository = userAccountRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.emailChallengeRepository = emailChallengeRepository;
        this.refreshReceiptRepository = refreshReceiptRepository;
        this.syncEventRepository = syncEventRepository;
        this.authWriteAppService = authWriteAppService;
        this.syncWrites = syncWrites;
        this.transactions = transactions;
        this.events = events;
    }

    /**
     * 处理expire对应的受控业务操作。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 60000)
    public void expire() {
        transactions.plain(
                () -> {
                    // 1. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            refreshReceiptRepository.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(authWriteAppService.removeRefreshReceipt(row.entity().id()));
                    }
                    // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            syncEventRepository.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(syncWrites.removeSyncEvent(row.entity().id()));
                    }
                    // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
    }

    /**
     * 过期验证码清除密文，过期登录清除活动令牌摘要并同步撤销。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 60000)
    public void expireCredentials() {
        // 1. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.plain(
                () -> {
                    // 1. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            emailChallengeRepository.query(
                                    QueryValue.all("id", 100)
                                            .where(
                                                    "status",
                                                    "IN",
                                                    java.util.List.of("PENDING_SEND", "ISSUED"))
                                            .where("expires_at", "LT", now()))) {
                        Transactions.require(
                                authWriteAppService.saveEmailChallenge(
                                        new EmailChallengeAggregate(row.entity())
                                                .delivery("EXPIRED")));
                    }
                    // 2. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                loginSessionRepository.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "ACTIVE")
                                .where("refresh_expires_at", "LT", now()))) {
            if (!"ACTIVE"
                    .equals(
                            userAccountRepository
                                    .findById(row.entity().userId())
                                    .entity()
                                    .status())) {
                continue;
            }
            transactions.mutate(
                    row.entity().userId(),
                    account -> {
                        // 1. 按可信内部标识读取登录会话当前快照。
                        var current =
                                loginSessionRepository.findById(row.entity().id()).entity();
                        // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                        if ("ACTIVE".equals(current.status())
                                && current.refreshExpiresAt().isBefore(now())) {
                            Transactions.require(
                                    authWriteAppService.saveLoginSession(
                                            new LoginSessionAggregate(current).revoke("EXPIRED")));
                        }
                        // 3. 同事务记录用户提交序号与同步事件，推送不能代替持久化。
                        events.append(
                                account,
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

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
