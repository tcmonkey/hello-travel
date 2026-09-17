package com.hellotravel.start.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 大整数按字符串传输，浏览器游标不会损失BIGINT精度。
 *
 * @author AIGenerator
 */
@Configuration
public class ProtocolConfiguration {

    /**
     * 将BIGINT协议值按字符串输出，避免浏览器精度损失。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer integerStrings() {
        return builder ->
                builder.serializerByType(Long.class, ToStringSerializer.instance)
                        .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
