package com.hellotravel.adaptor.auth.input.scheduler;

import com.hellotravel.application.auth.RetentionAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 触发认证凭据和同步保留期清理的输入适配器。
 *
 * @author AIGenerator
 */
@Component
public final class RetentionScheduler {

  /**
   * 记录保留期清理失败，避免中断后续周期。
   *
   * @author AIGenerator
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RetentionScheduler.class);

  private final RetentionAppService retentionAppService;

  /**
   * 注入受控保留期应用处理。
   *
   * @param retentionAppService 认证与同步的保留期处理协作
   * @author AIGenerator
   */
  public RetentionScheduler(RetentionAppService retentionAppService) {
    this.retentionAppService = retentionAppService;
  }

  /**
   * 清理已过期的刷新消费记录和同步事件。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 60000)
  public void expireRetainedRecords() {
    try {
      // 1. 委托应用层处理已过期的持久记录。
      retentionAppService.expire();
    } catch (Exception exception) {
      // 2. 记录本轮失败，避免定时触发器向外抛出异常。
      LOGGER.warn("保留记录清理任务失败", exception);
    }
  }

  /**
   * 清理过期验证码并撤销过期会话。
   *
   * @author AIGenerator
   */
  @Scheduled(fixedDelay = 60000)
  public void expireCredentials() {
    try {
      // 1. 委托应用层处理认证凭据生命周期。
      retentionAppService.expireCredentials();
    } catch (Exception exception) {
      // 2. 记录本轮失败，下一周期可继续扫描持久状态。
      LOGGER.warn("认证凭据清理任务失败", exception);
    }
  }
}
