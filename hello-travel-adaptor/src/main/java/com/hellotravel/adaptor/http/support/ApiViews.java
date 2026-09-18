package com.hellotravel.adaptor.http.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Component;

/**
 * 应用结果到独立公开协议的转换，不把内部实体或PO交给浏览器。
 *
 * @author AIGenerator
 */
@Component
public final class ApiViews {

    private final ObjectMapper mapper;

    public ApiViews(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 转换应用视图为独立公开协议。
     *
     * @author AIGenerator
     * @param value 受控业务载荷
     * @param type 目标协议类型
     * @param <T> 业务载荷类型
     * @return 当前操作的业务结果
     */
    public <T> T convert(Object value, Class<T> type) {
        return mapper.convertValue(value, type);
    }

    /**
     * 将应用Result转换为公开协议，失败保留错误分类与HTTP状态。
     *
     * @param result 已标准化应用结果
     * @param type 目标协议类型
     * @param <T> 公开协议类型
     * @return 独立公开协议结果
     * @author AIGenerator
     */
    public <T> Result<T> respond(Result<?> result, Class<T> type) {
        // 1. 依据下层标准结果的成功状态处理分支，避免继续使用无效数据。
        if (!result.success()) {
            return HttpResults.failure(result);
        }
        // 2. 将本层成功数据封装为标准结果，保持对外模型隔离。
        return Result.success(convert(result.data(), type));
    }
}
