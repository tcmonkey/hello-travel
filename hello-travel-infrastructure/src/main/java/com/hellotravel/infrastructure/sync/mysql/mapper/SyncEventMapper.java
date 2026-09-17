package com.hellotravel.infrastructure.sync.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.sync.mysql.pojo.SyncEventPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_sync_event的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface SyncEventMapper extends BaseMapper<SyncEventPO> {
}
