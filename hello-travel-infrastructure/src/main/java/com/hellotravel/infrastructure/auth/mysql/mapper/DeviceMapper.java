package com.hellotravel.infrastructure.auth.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.DevicePO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_device的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface DeviceMapper extends BaseMapper<DevicePO> {
}
