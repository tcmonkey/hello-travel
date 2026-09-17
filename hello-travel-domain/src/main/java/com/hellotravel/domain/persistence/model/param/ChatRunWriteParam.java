package com.hellotravel.domain.persistence.model.param;

/**
 * ChatRun完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record ChatRunWriteParam(
        com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate aggregate) {
        }
