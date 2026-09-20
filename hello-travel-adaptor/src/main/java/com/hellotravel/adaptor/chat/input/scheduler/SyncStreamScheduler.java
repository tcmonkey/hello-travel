package com.hellotravel.adaptor.chat.input.scheduler;

import com.hellotravel.adaptor.chat.input.stream.SyncStreamPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 按固定节奏触发对话域SSE事件发布，不承载HTTP路由或业务查询。
 *
 * @author AIGenerator
 */
@Component
public final class SyncStreamScheduler {

  /**
   * 记录异步发布失败，避免调度器向框架传播异常。
   *
   * @author AIGenerator
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(SyncStreamScheduler.class);

  private final SyncStreamPublisher publisher;

  /**
   * 注入对话域SSE发布器。
   *
   * @param publisher 有界订阅发布器
   * @author AIGenerator
   */
  public SyncStreamScheduler(SyncStreamPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * 触发当前订阅的事件补齐与心跳发送。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 1000)
  public void publish() {
    try {
      // 1. 调用传输发布器处理有界订阅，调度器不解释业务事件。
      publisher.publishPendingEvents();
    } catch (Exception exception) {
      // 2. 记录本轮调度失败，下一周期继续执行且不向调度框架抛出异常。
      LOGGER.warn("SSE事件发布任务失败", exception);
    }
  }
}
