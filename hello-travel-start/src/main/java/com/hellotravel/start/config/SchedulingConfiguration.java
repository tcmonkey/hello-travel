package com.hellotravel.start.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 默认开启后台调度；离线装配验证可以明确禁用外部任务。
 *
 * @author AIGenerator
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "hello-travel.workers.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SchedulingConfiguration {
}
