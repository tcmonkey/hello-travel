package com.hellotravel.domain.persistence.model.param;

/**
 * EmailChallenge完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record EmailChallengeWriteParam(
        com.hellotravel.domain.auth.model.aggregate.EmailChallengeAggregate aggregate) {
        }
