package com.hellotravel.domain.persistence.model.param;

import com.hellotravel.domain.auth.model.aggregate.UserAccountAggregate;

/**
 * UserAccount完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record UserAccountWriteParam(UserAccountAggregate aggregate) {
}
