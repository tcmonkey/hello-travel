package com.hellotravel.domain.persistence.model.param;

/**
 * Message完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MessageWriteParam(
        com.hellotravel.domain.chat.model.aggregate.MessageAggregate aggregate) {
        }
