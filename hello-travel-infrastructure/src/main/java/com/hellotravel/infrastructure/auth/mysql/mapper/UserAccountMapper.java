package com.hellotravel.infrastructure.auth.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.UserAccountPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_user_account的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountPO> {
}
