package com.hellotravel.domain.knowledge.repository;

import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.query.model.value.QueryValue;

import java.util.List;

/**
 * KnowledgeChunk聚合持久化端口；查询条件由可信业务生成。
 *
 * @author AIGenerator
 */
public interface KnowledgeChunkRepository {

    /**
     * 按内部主键恢复完整聚合；不存在时返回空值。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    KnowledgeChunkAggregate findById(Long id);

    /**
     * 按可信条件读取有界聚合集合。
     *
     * @author AIGenerator
     * @param queryValue 可信有界查询条件
     * @return 当前操作的业务结果
     */
    List<KnowledgeChunkAggregate> query(QueryValue queryValue);

    /**
     * 保存完整聚合，更新采用版本比较并交换。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    Boolean save(KnowledgeChunkAggregate aggregate);

    /**
     * 物理清理指定记录。
     *
     * @author AIGenerator
     * @param id 可信内部主键
     * @return 当前操作的业务结果
     */
    Boolean remove(Long id);
}
