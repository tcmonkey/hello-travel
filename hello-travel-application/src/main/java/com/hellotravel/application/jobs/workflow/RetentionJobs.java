package com.hellotravel.application.jobs.workflow;

import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.tx.Transactions;
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

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final com.hellotravel.application.sync.workflow.SyncEvents events;

    public RetentionJobs(
            TravelRepositories repositories,
            com.hellotravel.application.persistence.DomainWrites writes,
            Transactions transactions,
            com.hellotravel.application.sync.workflow.SyncEvents events) {
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
                    for (var row :
                            repositories.refreshReceipt.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(writes.removeRefreshReceipt(row.entity().id()));
                    }
                    for (var row :
                            repositories.syncEvent.query(
                                    QueryValue.all("id", 200).where("expires_at", "LT", now()))) {
                        Transactions.require(writes.removeSyncEvent(row.entity().id()));
                    }
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
        transactions.plain(
                () -> {
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
                                        new com.hellotravel.domain.auth.model.aggregate
                                                .EmailChallengeAggregate(
                                                row.entity().delivery("EXPIRED"))));
                    }
                    return null;
                });
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
                        var current =
                                repositories.loginSession.findById(row.entity().id()).entity();
                        if ("ACTIVE".equals(current.status())
                                && current.refreshExpiresAt().isBefore(now())) {
                            Transactions.require(
                                    writes.saveLoginSession(
                                            new com.hellotravel.domain.auth.model.aggregate
                                                    .LoginSessionAggregate(
                                                    current.revoke("EXPIRED"))));
                        }
                        events.append(
                                account,
                                "session.revoked",
                                current.publicId(),
                                current.version() + 1,
                                current.publicId(),
                                "{}");
                        return null;
                    });
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
