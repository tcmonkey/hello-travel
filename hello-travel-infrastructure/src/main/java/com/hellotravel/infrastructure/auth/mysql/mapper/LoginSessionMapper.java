package com.hellotravel.infrastructure.auth.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.LoginSessionPO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_login_session的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface LoginSessionMapper extends BaseMapper<LoginSessionPO> {
}
