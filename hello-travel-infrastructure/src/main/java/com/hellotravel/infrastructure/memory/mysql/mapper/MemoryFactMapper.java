package com.hellotravel.infrastructure.memory.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.memory.mysql.pojo.MemoryFactPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_memory_fact的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface MemoryFactMapper extends BaseMapper<MemoryFactPO> {
}
