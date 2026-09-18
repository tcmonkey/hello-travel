package com.hellotravel.domain.query.model.value;

import java.util.ArrayList;
import java.util.List;

/**
 * 有界域内查询条件；列名只能由服务端白名单产生。
 *
 * @param filters 过滤集合
 * @param order 排序列
 * @param descending 是否降序
 * @param limit 最大读取条数
 * @author AIGenerator
 */
public record QueryValue(List<FilterValue> filters, String order, boolean descending, int limit) {

    /**
     * 校验不可变值的边界并防御性复制输入集合。
     *
     * @author AIGenerator
     */
    public QueryValue {
        // 1. 保存独立的条件列表，调用者不能改变已建立的查询。
        filters = List.copyOf(filters);
        // 2. 固定单次查询数量上限，禁止不受限的窗口。
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("query limit");
        }
    }

    /**
     * 建立有界域内查询。
     *
     * @author AIGenerator
     * @param order 受控order参数
     * @param limit 受控limit参数
     * @return 当前操作的业务结果
     */
    public static QueryValue all(String order, int limit) {
        return new QueryValue(List.of(), order, false, limit);
    }

    /**
     * 追加结构化绑定条件。
     *
     * @author AIGenerator
     * @param column 服务端白名单列名
     * @param operator 允许的结构化条件操作符
     * @param value 受控业务载荷
     * @return 当前操作的业务结果
     */
    public QueryValue where(String column, String operator, Object value) {
        // 1. 复制已有过滤条件，追加条件不修改原查询。
        List<FilterValue> next = new ArrayList<>(filters);
        // 2. 由过滤值对象验证列名和操作符后，追加新的绑定条件。
        next.add(new FilterValue(column, operator, value));
        // 3. 返回保留原排序与数量边界的新查询快照。
        return new QueryValue(next, order, descending, limit);
    }

    /**
     * 返回降序查询条件。
     *
     * @author AIGenerator
     * @return 当前操作的业务结果
     */
    public QueryValue desc() {
        return new QueryValue(filters, order, true, limit);
    }
}
