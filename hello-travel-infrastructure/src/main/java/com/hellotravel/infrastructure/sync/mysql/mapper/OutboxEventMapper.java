package com.hellotravel.infrastructure.sync.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.sync.mysql.pojo.OutboxEventPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_outbox_event的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventPO> {
}
