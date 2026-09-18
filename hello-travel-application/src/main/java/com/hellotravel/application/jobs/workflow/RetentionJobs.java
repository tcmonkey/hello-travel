package com.hellotravel.application.jobs.workflow;

import com.hellotravel.application.persistence.DomainWrites;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.sync.workflow.SyncEvents;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.auth.model.aggregate.LoginSessionAggregate;
import com.hellotravel.domain.query.model.value.QueryValue;

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
public final class RetentionJobs {

    private final TravelRepositories repositories;

    private final DomainWrites writes;

    private final Transactions transactions;

    private final SyncEvents events;

    public RetentionJobs(
            TravelRepositories repositories,
            DomainWrites writes,
            Transactions transactions,
            SyncEvents events) {
        this.repositories = repositories;
        this.writes = writes;
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
                            repositories.refreshReceipt.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(writes.removeRefreshReceipt(row.entity().id()));
                    }
                    // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
                    for (var row :
                            repositories.syncEvent.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(writes.removeSyncEvent(row.entity().id()));
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
                            repositories.emailChallenge.query(
                                    QueryValue.all("id", 100)
                                            .where(
                                                    "status",
                                                    "IN",
                                                    java.util.List.of("PENDING_SEND", "ISSUED"))
                                            .where("expires_at", "LT", now()))) {
                        Transactions.require(
                                writes.saveEmailChallenge(
                                        new EmailChallengeAggregate(row.entity())
                                                .delivery("EXPIRED")));
                    }
                    // 2. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                    return null;
                });
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                repositories.loginSession.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "ACTIVE")
                                .where("refresh_expires_at", "LT", now()))) {
            if (!"ACTIVE"
                    .equals(
                            repositories
                                    .userAccount
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
                                repositories.loginSession.findById(row.entity().id()).entity();
                        // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                        if ("ACTIVE".equals(current.status())
                                && current.refreshExpiresAt().isBefore(now())) {
                            Transactions.require(
                                    writes.saveLoginSession(
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
