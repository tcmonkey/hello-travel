package com.hellotravel.application.jobs.workflow;

import com.hellotravel.application.knowledge.workflow.KnowledgeIndexer;
import com.hellotravel.application.mail.adaptor.MailOutAdaptor;
import com.hellotravel.application.mail.command.MailCommand;
import com.hellotravel.application.memory.workflow.MemoryFlow;
import com.hellotravel.application.memory.workflow.PrivacyCleanup;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.application.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.security.command.SecurityCommand;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.travel.workflow.RunCoordinator;
import com.hellotravel.application.travel.workflow.TravelGraph;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.query.model.value.QueryValue;
import com.hellotravel.domain.sync.model.aggregate.OutboxEventAggregate;
import com.hellotravel.domain.sync.model.entity.OutboxEventEntity;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 单机有界执行器与数据库领取；模型和邮件未知结果不盲目重放。
 *
 * @author AIGenerator
 */
@Component
public final class BackgroundJobs {

    private final TravelRepositories repositories;

    private final com.hellotravel.application.persistence.DomainWrites writes;

    private final Transactions transactions;

    private final TravelGraph graph;

    private final MemoryFlow memory;

    private final PrivacyCleanup privacy;

    private final KnowledgeIndexer indexer;

    private final RunCoordinator coordinator;

    private final SecurityOutAdaptor security;

    private final MailOutAdaptor mail;

    /**
     * 有界后台执行器。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.ThreadPoolExecutor executor =
            new java.util.concurrent.ThreadPoolExecutor(
                    2,
                    2,
                    0,
                    java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(16),
                    task -> {
                        Thread thread = new Thread(task, "ht-background");
                        thread.setDaemon(true);
                        return thread;
                    },
                    new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    public BackgroundJobs(
            com.hellotravel.application.persistence.DomainWrites writes,
            TravelRepositories repositories,
            Transactions transactions,
            TravelGraph graph,
            MemoryFlow memory,
            PrivacyCleanup privacy,
            KnowledgeIndexer indexer,
            RunCoordinator coordinator,
            SecurityOutAdaptor security,
            MailOutAdaptor mail) {
        this.writes = writes;
        this.repositories = repositories;
        this.transactions = transactions;
        this.graph = graph;
        this.memory = memory;
        this.privacy = privacy;
        this.indexer = indexer;
        this.coordinator = coordinator;
        this.security = security;
        this.mail = mail;
    }

    /**
     * 领取有界待派发任务并提交后台执行器。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 500)
    public void dispatch() {
        if (executor.getQueue().remainingCapacity() < 4) {
            return;
        }
        for (var row :
                repositories.outboxEvent.query(
                        QueryValue.all("id", 4)
                                .where("status", "EQ", "PENDING")
                                .where("next_attempt_at", "LE", now()))) {
            var claimed =
                    transactions.plain(
                            () -> {
                                var old =
                                        repositories
                                                .outboxEvent
                                                .findById(row.entity().id())
                                                .entity();
                                if (!"PENDING".equals(old.status())) {
                                    return null;
                                }
                                var next =
                                        new OutboxEventEntity(
                                                old.id(),
                                                old.publicId(),
                                                old.userId(),
                                                old.eventType(),
                                                old.dedupeKey(),
                                                old.payloadJson(),
                                                "DELIVERING",
                                                old.attemptCount() + 1,
                                                old.maxAttempts(),
                                                old.nextAttemptAt(),
                                                Ids.next(),
                                                old.leaseFence() + 1,
                                                now().plusMinutes(15),
                                                old.deliveredAt(),
                                                old.errorCode(),
                                                old.createdAt(),
                                                old.updatedAt(),
                                                old.version());
                                Transactions.require(
                                        writes.saveOutboxEvent(new OutboxEventAggregate(next)));
                                return repositories.outboxEvent.findById(next.id()).entity();
                            });
            if (claimed != null) {
                try {
                    executor.execute(() -> deliver(claimed));
                } catch (java.util.concurrent.RejectedExecutionException exception) {
                    complete(claimed, false, "CAPACITY");
                }
            }
        }
        for (var row :
                repositories.indexJob.query(
                        QueryValue.all("id", 2).where("status", "EQ", "PENDING"))) {
            try {
                executor.execute(() -> indexer.execute(row.entity().id()));
            } catch (java.util.concurrent.RejectedExecutionException exception) {
                return;
            }
        }
    }

    private void deliver(OutboxEventEntity event) {
        try {
            var payload = Json.read(event.payloadJson());
            switch (event.eventType()) {
                case "SYNC" -> {
                    /* SSE通过持久事件游标读取，发件箱确认不替代事件事实源。 */
                }
                case "GENERATE" -> graph.execute(payload.path("runId").asText());
                case "MEMORY_EXTRACT" -> memory.extract(payload.path("runId").asText());
                case "MEMORY_REBUILD" -> privacy.execute(payload.path("conversationId").asLong());
                case "EMAIL" -> email(payload.path("challengeId").asText());
                default -> throw new IllegalArgumentException("unknown event");
            }
            complete(event, true, null);
        } catch (Exception exception) {
            complete(event, false, "DELIVERY_FAILED");
        }
    }

    private void email(String id) {
        var rows =
                repositories.emailChallenge.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", id));
        if (rows.isEmpty()) {
            return;
        }
        var challenge = rows.get(0).entity();
        if (!"PENDING_SEND".equals(challenge.status()) || challenge.expiresAt().isBefore(now())) {
            return;
        }
        String encrypted =
                java.util.Base64.getEncoder().encodeToString(challenge.deliveryCiphertext());
        var decrypted = security.process(new SecurityCommand("DECRYPT", encrypted, null, id));
        boolean sent =
                decrypted.success()
                        && mail.deliver(
                                        new MailCommand(
                                                challenge.emailNormalized(),
                                                decrypted.data().value(),
                                                id))
                                .success();
        transactions.plain(
                () -> {
                    var current = repositories.emailChallenge.findById(challenge.id()).entity();
                    if ("PENDING_SEND".equals(current.status())) {
                        Transactions.require(
                                writes.saveEmailChallenge(
                                        new EmailChallengeAggregate(
                                                current.delivery(sent ? "ISSUED" : "FAILED"))));
                    }
                    return null;
                });
        if (!sent) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
    }

    private void complete(OutboxEventEntity expected, boolean success, String error) {
        transactions.plain(
                () -> {
                    var old = repositories.outboxEvent.findById(expected.id()).entity();
                    if ("DELIVERING".equals(old.status())
                            && old.leaseFence().equals(expected.leaseFence())) {
                        boolean retrySafe =
                                !success
                                        && java.util.Set.of("SYNC", "MEMORY_REBUILD")
                                                .contains(old.eventType())
                                        && old.attemptCount() < old.maxAttempts();
                        var next =
                                new OutboxEventEntity(
                                        old.id(),
                                        old.publicId(),
                                        old.userId(),
                                        old.eventType(),
                                        old.dedupeKey(),
                                        old.payloadJson(),
                                        success ? "DELIVERED" : retrySafe ? "PENDING" : "DEAD",
                                        old.attemptCount(),
                                        old.maxAttempts(),
                                        retrySafe
                                                ? now().plusSeconds(
                                                                Math.min(
                                                                        300,
                                                                        1L
                                                                                << Math.min(
                                                                                        8,
                                                                                        old
                                                                                                .attemptCount())))
                                                : old.nextAttemptAt(),
                                        old.leaseOwner(),
                                        old.leaseFence(),
                                        null,
                                        success ? now() : null,
                                        error,
                                        old.createdAt(),
                                        old.updatedAt(),
                                        old.version());
                        Transactions.require(
                                writes.saveOutboxEvent(new OutboxEventAggregate(next)));
                    }
                    return null;
                });
    }

    /**
     * 停止未知投递结果并恢复过期生成任务。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 30000)
    public void recovery() {
        coordinator.recoverExpired();
        coordinator.recoverAccepted();
        for (var row :
                repositories.outboxEvent.query(
                        QueryValue.all("id", 50)
                                .where("status", "EQ", "DELIVERING")
                                .where("lease_until", "LT", now()))) {
            complete(row.entity(), false, "DELIVERY_UNKNOWN");
        }
    }

    /**
     * 对账项目私有索引并恢复未知任务状态。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 60000)
    public void reconcile() {
        if (executor.getQueue().remainingCapacity() > 2) {
            try {
                executor.execute(indexer::reconcile);
            } catch (java.util.concurrent.RejectedExecutionException exception) {
                return;
            }
        }
    }

    /**
     * 停止本机后台执行器。
     *
     * @author AIGenerator
     */
    @jakarta.annotation.PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
