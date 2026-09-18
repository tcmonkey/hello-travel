package com.hellotravel.adaptor.http.support;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * 在反序列化之前限制JSON正文，包括未提供Content-Length的分块请求。
 *
 * @author AIGenerator
 */
@ControllerAdvice
public final class JsonBodyLimit extends RequestBodyAdviceAdapter {

    /**
     * 处理supports对应的受控业务操作。
     *
     * @author AIGenerator
     * @param methodParameter 受控methodParameter参数
     * @param targetType 受控targetType参数
     * @param converterType 受控converterType参数
     * @return 当前操作的业务结果
     */
    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    /**
     * 处理beforeBodyRead对应的受控业务操作。
     *
     * @author AIGenerator
     * @param inputMessage 受控inputMessage参数
     * @param parameter 受控parameter参数
     * @param targetType 受控targetType参数
     * @param converterType 受控converterType参数
     * @return 当前操作的业务结果
     */
    @Override
    public HttpInputMessage beforeBodyRead(
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        // 1. 取得当前请求或响应正文，供本段后续处理使用。
        byte[] body = inputMessage.getBody().readNBytes(32769);
        // 2. 核对格式、长度或数量边界，失败中止当前处理。
        if (body.length > 32768) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
        // 3. 返回本段实际处理结果，保持本层输出契约。
        return new HttpInputMessage() {

            /**
             * 处理getBody对应的受控业务操作。
             *
             * @author AIGenerator
             * @return 当前操作的业务结果
             */
            public java.io.InputStream getBody() {
                return new java.io.ByteArrayInputStream(body);
            }

            /**
             * 处理getHeaders对应的受控业务操作。
             *
             * @author AIGenerator
             * @return 当前操作的业务结果
             */
            public org.springframework.http.HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }
}
