package com.hellotravel.start.config;

import com.hellotravel.domain.annotation.DomainService;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * 按领域专用注解注册领域服务，领域模块保持不依赖Spring。
 *
 * @author AIGenerator
 */
@Configuration
@ComponentScan(
        basePackages = "com.hellotravel.domain",
        useDefaultFilters = false,
        includeFilters =
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = DomainService.class))
public class DomainServiceScanConfiguration {
}
