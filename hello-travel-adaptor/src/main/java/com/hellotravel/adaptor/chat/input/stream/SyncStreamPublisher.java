package com.hellotravel.adaptor.chat.input.stream;

import com.hellotravel.adaptor.auth.input.assembler.AuthAssembler;
import com.hellotravel.adaptor.chat.input.assembler.SyncAssembler;
import com.hellotravel.adaptor.common.HttpResults;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.auth.AuthAppService;
import com.hellotravel.application.chat.SyncAppService;
import com.hellotravel.common.identity.Ids;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 管理SSE订阅及其有界事件推送，不承担HTTP路由或时间触发。
 *
 * @author AIGenerator
 */
@Component
public final class SyncStreamPublisher {

  private final AuthAppService auth;
  private final SyncAppService sync;

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
            // 1. 为受限发送池建立独立线程，避免阻塞后台调度。
            Thread thread = new Thread(task, "ht-sse");
            // 2. 标记为守护线程，进程退出不被空闲后台线程阻塞。
            thread.setDaemon(true);
            // 3. 返回已配置线程，由有界执行器管理任务并发。
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

  private final AuthAssembler authAssembler;
  private final SyncAssembler syncAssembler;

  public SyncStreamPublisher(
      AuthAppService auth,
      SyncAppService sync,
      AuthAssembler authAssembler,
      SyncAssembler syncAssembler) {
    this.auth = auth;
    this.sync = sync;
    this.authAssembler = authAssembler;
    this.syncAssembler = syncAssembler;
  }

  /**
   * 注册绑定原页面登录的有界SSE订阅。
   *
   * @author AIGenerator
   * @param userId 已由HTTP身份解析的账号主键
   * @param sessionId 原页面会话标识
   * @param accessToken 原请求访问令牌
   * @param after 已处理的持久事件序号
   * @return 已注册的SSE订阅
   */
  public synchronized SseEmitter subscribe(
      Long userId, String sessionId, String accessToken, long after) {
    // 1. 核对分页游标与订阅上限，防止无界连接或无法推进的恢复。
    if (after < 0
        || subscriptions.size() >= 100
        || subscriptions.values().stream().filter(x -> x.user.equals(userId)).count() >= 4) {
      throw new AdaptorException(AdaptorErrorCode.RATE_LIMITED);
    }
    // 2. 生成本次订阅的公开标识，内部状态保持独立。
    String id = Ids.next();
    SseEmitter emitter = new SseEmitter(60000L);
    var subscription = new Subscription(userId, sessionId, accessToken, after, emitter);
    // 3. 保存订阅并注册连接生命周期回调。
    subscriptions.put(id, subscription);
    emitter.onCompletion(() -> subscriptions.remove(id));
    emitter.onTimeout(
        () -> {
          // 1. 持久化当前完整聚合，失败必须中断事务而非继续提交。
          subscriptions.remove(id);
          // 2. 执行complete职责步骤，并把失败交给所属事务或入口处理。
          emitter.complete();
        });
    emitter.onError(error -> subscriptions.remove(id));
    // 4. 返回已注册的订阅，由Scheduler触发后续事件发送。
    return emitter;
  }

  /**
   * 读取订阅游标后的持久事件并检查登录仍有效。
   *
   * @author AIGenerator
   */
  public void publishPendingEvents() {
    // 1. 按订阅逐个申请发送槽，异步拉取持久事件；忙碌或断开的订阅不重复调度。
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
      // 1. 将订阅原始SID和令牌转换为检查命令，失效时停止发送。
      var authCommand = authAssembler.check(sub.sid, sub.access, "stream");
      var authResult = auth.authenticate(authCommand);
      HttpResults.required(authResult);
      // 2. 转换补齐命令并核验应用结果，保持持久游标连续性。
      var syncCommand = syncAssembler.toCommand(sub.user, sub.after);
      var syncResult = sync.synchronize(syncCommand);
      var result = HttpResults.required(syncResult);
      // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
      for (var event : result.items()) {
        sub.emitter.send(
            SseEmitter.event()
                .id(Long.toString(event.seq()))
                .name("sync")
                .data(syncAssembler.event(event)));
        sub.after = event.seq();
      }
      // 4. 无待发送事件时发送心跳，保持连接而不推进事件游标。
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
}
