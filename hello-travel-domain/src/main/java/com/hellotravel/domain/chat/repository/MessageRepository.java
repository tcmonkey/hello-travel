package com.hellotravel.domain.chat.repository;

import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.query.model.value.QueryValue;

import java.util.List;

/**
 * Message聚合持久化端口；查询条件由可信业务生成。
 *
 * @author AIGenerator
 */
public interface MessageRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    MessageAggregate findById(Long id);

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    List<MessageAggregate> query(QueryValue queryValue);

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    Boolean save(MessageAggregate aggregate);

    /**
     * 物理清理指定记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    Boolean remove(Long id);
}
