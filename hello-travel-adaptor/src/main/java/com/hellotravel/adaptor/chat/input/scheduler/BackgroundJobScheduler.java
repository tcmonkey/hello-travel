package com.hellotravel.adaptor.chat.input.scheduler;

import com.hellotravel.application.chat.OutboxDispatchAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 触发对话域异步任务、运行恢复和知识索引对账的输入适配器。
 *
 * @author AIGenerator
 */
@Component
public final class BackgroundJobScheduler {

  /**
   * 记录后台触发失败，保留下一周期的恢复机会。
   *
   * @author AIGenerator
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobScheduler.class);

  private final OutboxDispatchAppService outboxDispatchAppService;

  /**
   * 注入应用层后台任务协调能力。
   *
   * @param outboxDispatchAppService 负责领取和执行已持久化任务的应用协作
   * @author AIGenerator
   */
  public BackgroundJobScheduler(OutboxDispatchAppService outboxDispatchAppService) {
    this.outboxDispatchAppService = outboxDispatchAppService;
  }

  /**
   * 触发一次有界待派发任务领取。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 500)
  public void dispatchPending() {
    try {
      // 1. 委托应用层领取持久化待办，调度器不处理任务内容。
      outboxDispatchAppService.dispatch();
    } catch (Exception exception) {
      // 2. 记录本轮失败，后续周期依据持久状态继续恢复。
      LOGGER.warn("后台任务派发失败", exception);
    }
  }

  /**
   * 触发生成任务和失效领取的恢复。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 30000)
  public void recoverPending() {
    try {
      // 1. 委托应用层执行恢复策略。
      outboxDispatchAppService.recovery();
    } catch (Exception exception) {
      // 2. 记录本轮失败，避免异常终止后续调度。
      LOGGER.warn("后台任务恢复失败", exception);
    }
  }

  /**
   * 触发知识索引的一致性检查。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 60000)
  public void reconcileKnowledgeIndex() {
    try {
      // 1. 委托应用层处理索引对账，不在输入适配器访问仓储或向量端口。
      outboxDispatchAppService.reconcile();
    } catch (Exception exception) {
      // 2. 记录本轮失败，下一周期继续基于持久任务重试。
      LOGGER.warn("知识索引对账任务失败", exception);
    }
  }
}
