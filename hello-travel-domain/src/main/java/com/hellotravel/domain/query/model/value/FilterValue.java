package com.hellotravel.domain.query.model.value;

/**
 * 结构化过滤，禁止SQL片段。
 *
 * @param column 可信列名
 * @param operator 有限条件操作符
 * @param value 绑定参数
 * @author AIGenerator
 */
public record FilterValue(String column, String operator, Object value) {
}
