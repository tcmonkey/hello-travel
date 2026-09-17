package com.hellotravel.infrastructure.chat.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.chat.mysql.pojo.MessagePO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_message的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessagePO> {
}
