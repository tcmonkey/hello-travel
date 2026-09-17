package com.hellotravel.infrastructure.chat.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.chat.mysql.pojo.ConversationPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_conversation的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationPO> {
}
