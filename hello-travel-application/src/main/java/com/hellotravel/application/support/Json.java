package com.hellotravel.application.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 有界内部状态JSON编码，外部协议由适配器另外转换。
 *
 * @author AIGenerator
 */
public final class Json {

    /**
     * 保存MAPPER对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 建立Json并保存明确业务依赖。
     *
     * @author AIGenerator
     */
    private Json() {
}

    /**
     * 处理encode对应的受控业务操作。
     *
     * @author AIGenerator
     * @param value 受控业务载荷
     * @return 当前操作的业务结果
     */
    public static String encode(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    /**
     * 处理read对应的受控业务操作。
     *
     * @author AIGenerator
     * @param value 受控业务载荷
     * @return 当前操作的业务结果
     */
    public static JsonNode read(String value) {
        try {
            return MAPPER.readTree(value == null ? "{}" : value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException(exception);
        }
    }
}
