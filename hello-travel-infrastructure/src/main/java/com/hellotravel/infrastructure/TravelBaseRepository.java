package com.hellotravel.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.hellotravel.domain.query.model.value.FilterValue;
import com.hellotravel.domain.query.model.value.QueryValue;

import java.util.Set;

/**
 * 公共标准CRUD与结构化条件转换。
 *
 * @param <M> Mapper类型
 * @param <T> PO类型
 * @author AIGenerator
 */
public abstract class TravelBaseRepository<M extends BaseMapper<T>, T>
        extends CrudRepository<M, T> {

    /**
     * 处理conditions对应的受控业务操作。
     *
     * @author AIGenerator
     * @param query 受控query参数
     * @param columns 受控columns参数
     * @return 当前操作的业务结果
     */
    protected QueryWrapper<T> conditions(QueryValue query, Set<String> columns) {
        QueryWrapper<T> result = new QueryWrapper<>();
        if (!columns.contains(query.order())) {
            throw new IllegalArgumentException("column");
        }
        for (FilterValue filter : query.filters()) {
            if (!columns.contains(filter.column())) {
                throw new IllegalArgumentException("column");
            }
            switch (filter.operator()) {
                case "EQ" -> result.eq(filter.column(), filter.value());
                case "GT" -> result.gt(filter.column(), filter.value());
                case "GE" -> result.ge(filter.column(), filter.value());
                case "LT" -> result.lt(filter.column(), filter.value());
                case "LE" -> result.le(filter.column(), filter.value());
                case "NE" -> result.ne(filter.column(), filter.value());
                case "NULL" -> result.isNull(filter.column());
                case "IN" -> result.in(filter.column(), (java.util.Collection<?>) filter.value());
                default -> throw new IllegalArgumentException("operator");
            }
        }
        result.orderBy(true, !query.descending(), query.order());
        return result;
    }
}
