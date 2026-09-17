package com.hellotravel.domain.persistence.model.param;

/**
 * Conversation完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record ConversationWriteParam(
        com.hellotravel.domain.chat.model.aggregate.ConversationAggregate aggregate) {
        }
