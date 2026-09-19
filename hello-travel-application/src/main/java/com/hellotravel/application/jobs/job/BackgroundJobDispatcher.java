package com.hellotravel.application.jobs.job;

import com.hellotravel.application.auth.support.AuthRepositories;
import com.hellotravel.application.auth.support.AuthWrites;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.support.KnowledgeRepositories;
import com.hellotravel.application.knowledge.job.KnowledgeIndexJob;
import com.hellotravel.application.auth.email.adaptor.MailOutAdaptor;
import com.hellotravel.application.auth.email.assembler.MailCommandAssembler;
import com.hellotravel.application.chat.memory.cleanup.PrivacyCleanupService;
import com.hellotravel.application.chat.memory.context.MemoryContextService;
import com.hellotravel.application.auth.security.adaptor.SecurityOutAdaptor;
import com.hellotravel.application.auth.security.assembler.SecurityCommandAssembler;
import com.hellotravel.application.support.Json;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.application.chat.sync.support.SyncRepositories;
import com.hellotravel.application.chat.sync.support.SyncWrites;
import com.hellotravel.application.chat.travel.execution.RunExecutionService;
import com.hellotravel.application.chat.travel.service.TravelApplication;
import com.hellotravel.application.chat.travel.assembler.TravelApplicationAssembler;
import com.hellotravel.application.tx.Transactions;
import com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate;
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
public final class BackgroundJobDispatcher {

    private final AuthRepositories authRepositories;
    private final KnowledgeRepositories knowledgeRepositories;
    private final SyncRepositories syncRepositories;
    private final AuthWrites authWrites;
    private final SyncWrites syncWrites;
    private final Transactions transactions;
    private final TravelApplication travelApplication;
    private final TravelApplicationAssembler travelApplicationAssembler;
    private final MemoryContextService memory;
    private final PrivacyCleanupService privacy;
    private final KnowledgeIndexJob indexer;
    private final RunExecutionService coordinator;
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
                        // 1. 为受限后台队列建立专用线程，不在HTTP请求内执行耗时任务。
                        Thread thread = new Thread(task, "ht-background");
                        // 2. 标记为守护线程，进程退出不被空闲后台线程阻塞。
                        thread.setDaemon(true);
                        // 3. 返回已配置线程，由有界执行器管理任务并发。
                        return thread;
                    },
                    new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    private final MailCommandAssembler mailCommandAssembler;
    private final SecurityCommandAssembler securityCommandAssembler;

    public BackgroundJobDispatcher(
            AuthWrites authWrites,
            SyncWrites syncWrites,
            AuthRepositories authRepositories,
            KnowledgeRepositories knowledgeRepositories,
            SyncRepositories syncRepositories,
            Transactions transactions,
            TravelApplication travelApplication,
            TravelApplicationAssembler travelApplicationAssembler,
            MemoryContextService memory,
            PrivacyCleanupService privacy,
            KnowledgeIndexJob indexer,
            RunExecutionService coordinator,
            SecurityOutAdaptor security,
            MailOutAdaptor mail,
            MailCommandAssembler mailCommandAssembler,
            SecurityCommandAssembler securityCommandAssembler) {
        this.authWrites = authWrites;
        this.syncWrites = syncWrites;
        this.authRepositories = authRepositories;
        this.knowledgeRepositories = knowledgeRepositories;
        this.syncRepositories = syncRepositories;
        this.transactions = transactions;
        this.travelApplication = travelApplication;
        this.travelApplicationAssembler = travelApplicationAssembler;
        this.memory = memory;
        this.privacy = privacy;
        this.indexer = indexer;
        this.coordinator = coordinator;
        this.security = security;
        this.mail = mail;
        this.mailCommandAssembler = mailCommandAssembler;
        this.securityCommandAssembler = securityCommandAssembler;
    }

    /**
     * 领取有界待派发任务并提交后台执行器。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 500)
    public void dispatch() {
        // 1. 队列余量不足时跳过本轮领取，后台任务不无限堆积。
        if (executor.getQueue().remainingCapacity() < 4) {
            return;
        }
        // 2. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                syncRepositories.outboxEvent.query(
                        QueryValue.all("id", 4)
                                .where("status", "EQ", "PENDING")
                                .where("next_attempt_at", "LE", now()))) {
            var claimed =
                    transactions.plain(
                            () -> {
                                // 1. 按可信内部标识读取发件箱任务当前快照。
                                var old =
                                        syncRepositories
                                                .outboxEvent
                                                .findById(row.entity().id())
                                                .entity();
                                // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                                if (!"PENDING".equals(old.status())) {
                                    return null;
                                }
                                // 3. 通过领域聚合语义准备业务快照，固定状态由实体封装。
                                var next =
                                        new OutboxEventAggregate(old)
                                                .claimed(
                                                        old.attemptCount() + 1,
                                                        old.leaseFence() + 1,
                                                        now().plusMinutes(15))
                                                .entity();
                                // 4. 持久化当前完整聚合，失败必须中断事务而非继续提交。
                                Transactions.require(
                                        syncWrites.saveOutboxEvent(new OutboxEventAggregate(next)));
                                // 5. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
                                return syncRepositories.outboxEvent.findById(next.id()).entity();
                            });
            if (claimed != null) {
                try {
                    executor.execute(() -> deliver(claimed));
                } catch (java.util.concurrent.RejectedExecutionException exception) {
                    complete(claimed, false, "CAPACITY");
                }
            }
        }
        // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                knowledgeRepositories.indexJob.query(
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
            // 1. 取得任务载荷的结构化内容，供本段后续处理使用。
            var payload = Json.read(event.payloadJson());
            // 2. 按可信业务动作分发独立分支，未知动作返回受控失败。
            switch (event.eventType()) {
                case "SYNC" -> {
                    /* SSE通过持久事件游标读取，发件箱确认不替代事件事实源。 */
                }
                case "GENERATE" -> ApplicationFailures.required(
                        travelApplication.generate(travelApplicationAssembler.generate(payload)));
                case "MEMORY_EXTRACT" -> memory.extract(payload.path("runId").asText());
                case "MEMORY_REBUILD" -> privacy.execute(payload.path("conversationId").asLong());
                case "EMAIL" -> email(payload.path("challengeId").asText());
                default -> throw new IllegalArgumentException("unknown event");
            }
            // 3. 执行complete职责步骤，并把失败交给所属事务或入口处理。
            complete(event, true, null);
        } catch (Exception exception) {
            complete(event, false, "DELIVERY_FAILED");
        }
    }

    private void email(String id) {
        // 1. 读取邮箱验证码，按当前用例条件限定查询窗口。
        var rows =
                authRepositories.emailChallenge.query(
                        QueryValue.all("id", 1).where("public_id", "EQ", id));
        // 2. 没有待处理任务时结束当前领取窗口。
        if (rows.isEmpty()) {
            return;
        }
        // 3. 取得待投递验证码的持久化快照，供本段后续处理使用。
        var challenge = rows.get(0).entity();
        // 4. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
        if (!"PENDING_SEND".equals(challenge.status()) || challenge.expiresAt().isBefore(now())) {
            return;
        }
        // 5. 准备可投递的加密验证码，原始码不进入数据库。
        String encrypted =
                java.util.Base64.getEncoder().encodeToString(challenge.deliveryCiphertext());
        var decrypted = security.process(securityCommandAssembler.decrypt(encrypted, id));
        boolean sent =
                decrypted.success()
                        && mail.deliver(mailCommandAssembler.challenge(challenge, decrypted.data()))
                                .success();
        // 6. 进入受控事务处理，结果与回滚责任保持清晰。
        transactions.plain(
                () -> {
                    // 1. 按可信内部标识读取邮箱验证码当前快照。
                    var current = authRepositories.emailChallenge.findById(challenge.id()).entity();
                    // 2. 依据实体当前状态与允许的操作处理分支，避免继续使用无效数据。
                    if ("PENDING_SEND".equals(current.status())) {
                        Transactions.require(
                                authWrites.saveEmailChallenge(
                                        new EmailChallengeAggregate(current)
                                                .delivery(sent ? "ISSUED" : "FAILED")));
                    }
                    // 3. 返回去空白、统一大小写的规范邮箱供唯一键与证明绑定使用。
                    return null;
                });
        // 7. 邮件未确认投递成功时登记可恢复失败，不能把任务标为已发送。
        if (!sent) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
    }

    private void complete(OutboxEventEntity expected, boolean success, String error) {
        transactions.plain(
                () -> {
                    // 1. 按可信内部标识读取发件箱任务当前快照。
                    var old = syncRepositories.outboxEvent.findById(expected.id()).entity();
                    // 2. 核对租约持有者、栅栏和到期时间，旧执行者不能提交。
                    if ("DELIVERING".equals(old.status())
                            && old.leaseFence().equals(expected.leaseFence())) {
                        boolean retrySafe =
                                !success
                                        && java.util.Set.of("SYNC", "MEMORY_REBUILD")
                                                .contains(old.eventType())
                                        && old.attemptCount() < old.maxAttempts();
                        var next =
                                old.completed(
                                        success ? "DELIVERED" : retrySafe ? "PENDING" : "DEAD",
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
                                        success ? now() : null,
                                        error);
                        Transactions.require(
                                syncWrites.saveOutboxEvent(new OutboxEventAggregate(next)));
                    }
                    // 3. 提供本事务或回调的处理结果，完成责任由所属外层流程承接。
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
        // 1. 执行recoverExpired职责步骤，并把失败交给所属事务或入口处理。
        coordinator.recoverExpired();
        // 2. 执行recoverAccepted职责步骤，并把失败交给所属事务或入口处理。
        coordinator.recoverAccepted();
        // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var row :
                syncRepositories.outboxEvent.query(
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
        // 1. 仅在执行器有余量时提交一致性修复任务，避免后台队列无限增长。
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
