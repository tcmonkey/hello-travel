package com.hellotravel.adaptor.http.stream;

import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.service.AuthApplication;
import com.hellotravel.application.sync.command.SyncCommand;
import com.hellotravel.application.sync.service.SyncApplication;
import com.hellotravel.common.identity.Ids;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE传输适配器返回流而非业务Result；有界连接，每轮校验原页面登录。
 *
 * @author AIGenerator
 */
@Controller
public final class SyncStreamController {

    private final AuthApplication auth;

    private final SyncApplication sync;

    /**
     * 有界SSE写入执行器，慢订阅不占用调度线程。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.ThreadPoolExecutor sender =
            new java.util.concurrent.ThreadPoolExecutor(
                    4,
                    4,
                    0,
                    java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(100),
                    task -> {
                        Thread thread = new Thread(task, "ht-sse");
                        thread.setDaemon(true);
                        return thread;
                    },
                    new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    /**
     * 连接上限及游标状态，断线通过持久事件补齐。
     *
     * @author AIGenerator
     */
    private final java.util.concurrent.ConcurrentMap<String, Subscription> subscriptions =
            new java.util.concurrent.ConcurrentHashMap<>();

    public SyncStreamController(AuthApplication auth, SyncApplication sync) {
        this.auth = auth;
        this.sync = sync;
    }

    /**
     * 建立绑定原页面登录的有界SSE订阅。
     *
     * @author AIGenerator
     * @param after 已处理的持久事件序号
     * @param request HTTP请求及认证上下文
     * @return 当前操作的业务结果
     */
    @GetMapping(value = "/api/v1/events", produces = "text/event-stream")
    @ResponseBody
    public synchronized SseEmitter subscribe(@RequestParam long after, HttpServletRequest request) {
        try {
            Long user = HttpIdentity.user(request);
            if (after < 0
                    || subscriptions.size() >= 100
                    || subscriptions.values().stream().filter(x -> x.user.equals(user)).count()
                            >= 4) {
                throw new DomainException(DomainErrorCode.RATE_LIMITED);
            }
            String id = Ids.next();
            SseEmitter emitter = new SseEmitter(60000L);
            var subscription =
                    new Subscription(
                            user,
                            request.getHeader("X-Session-ID"),
                            HttpIdentity.access(request),
                            after,
                            emitter);
            subscriptions.put(id, subscription);
            emitter.onCompletion(() -> subscriptions.remove(id));
            emitter.onTimeout(
                    () -> {
                        subscriptions.remove(id);
                        emitter.complete();
                    });
            emitter.onError(error -> subscriptions.remove(id));
            return emitter;
        } catch (Exception exception) {
            return failedSubscription(exception);
        }
    }

    /**
     * 读取订阅游标后的持久事件并检查登录仍有效。
     *
     * @author AIGenerator
     */
    @Scheduled(fixedDelay = 1000)
    public void poll() {
        for (var entry : subscriptions.entrySet()) {
            var sub = entry.getValue();
            if (!sub.busy.compareAndSet(false, true)) {
                continue;
            }
            try {
                sender.execute(() -> emit(entry.getKey(), sub));
            } catch (java.util.concurrent.RejectedExecutionException exception) {
                sub.busy.set(false);
                subscriptions.remove(entry.getKey());
                sub.emitter.complete();
            }
        }
    }

    private void emit(String id, Subscription sub) {
        try {
            com.hellotravel.adaptor.http.support.HttpResults.required(
                    auth.authenticate(
                            new AuthCommand(
                                    "CHECK",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    sub.sid,
                                    sub.access,
                                    null,
                                    null,
                                    "stream")));
            var result =
                    com.hellotravel.adaptor.http.support.HttpResults.required(
                            sync.synchronize(new SyncCommand(sub.user, sub.after, 100)));
            for (var event : result.items()) {
                sub.emitter.send(
                        SseEmitter.event().id(Long.toString(event.seq())).name("sync").data(event));
                sub.after = event.seq();
            }
            if (result.items().isEmpty()) {
                sub.emitter.send(SseEmitter.event().comment("keepalive"));
            }
        } catch (Exception exception) {
            subscriptions.remove(id);
            try {
                sub.emitter.send(SseEmitter.event().name("reset").data("reconnect"));
            } catch (java.io.IOException ignored) {
                /* 已断开的连接通过游标重连。 */
            }
            sub.emitter.complete();
        } finally {
            sub.busy.set(false);
        }
    }

    @jakarta.annotation.PreDestroy
    private void closeSender() {
        sender.shutdownNow();
    }

    /**
     * 承载Subscription的受控业务契约。
     *
     * @author AIGenerator
     */
    private static final class Subscription {

        /**
         * 当前连接在执行器中至多有一个有序写入任务。
         *
         * @author AIGenerator
         */
        private final java.util.concurrent.atomic.AtomicBoolean busy =
                new java.util.concurrent.atomic.AtomicBoolean();

        /**
         * 保存user对应的有界运行状态。
         *
         * @author AIGenerator
         */
        private final Long user;

        /**
         * 保存sid对应的有界运行状态。
         *
         * @author AIGenerator
         */
        private final String sid;

        /**
         * 保存access对应的有界运行状态。
         *
         * @author AIGenerator
         */
        private final String access;

        /**
         * 保存emitter对应的有界运行状态。
         *
         * @author AIGenerator
         */
        private final SseEmitter emitter;

        /**
         * 保存after对应的有界运行状态。
         *
         * @author AIGenerator
         */
        private long after;

        /**
         * 建立Subscription并保存明确业务依赖。
         *
         * @author AIGenerator
         * @param user 受控user参数
         * @param sid 受控sid参数
         * @param access 受控access参数
         * @param after 已处理的持久事件序号
         * @param emitter 受控emitter参数
         */
        private Subscription(Long user, String sid, String access, long after, SseEmitter emitter) {
            this.user = user;
            this.sid = sid;
            this.access = access;
            this.after = after;
            this.emitter = emitter;
        }
    }

    private SseEmitter failedSubscription(Exception exception) {
        var failure = com.hellotravel.adaptor.http.support.HttpResults.capture(exception);
        var emitter = new SseEmitter(1000L);
        try {
            emitter.send(SseEmitter.event().name("error").data(failure));
            emitter.complete();
        } catch (Exception sendFailure) {
            emitter.complete();
        }
        return emitter;
    }
}
