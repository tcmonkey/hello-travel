package com.hellotravel.infrastructure.auth.mysql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hellotravel.infrastructure.auth.mysql.pojo.EmailChallengePO;

import org.apache.ibatis.annotations.Mapper;

/**
 * ht_email_challenge的MyBatis-Plus标准CRUD映射。
 *
 * @author AIGenerator
 */
@Mapper
public interface EmailChallengeMapper extends BaseMapper<EmailChallengePO> {
}
