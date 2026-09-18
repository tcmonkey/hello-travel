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
        // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
        QueryWrapper<T> result = new QueryWrapper<>();
        // 2. 只允许仓储已声明的排序字段，禁止客户端传入任意数据库列。
        if (!columns.contains(query.order())) {
            throw new IllegalArgumentException("column");
        }
        // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
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
        // 4. 执行orderBy职责步骤，并把失败交给所属事务或入口处理。
        result.orderBy(true, !query.descending(), query.order());
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return result;
    }
}
