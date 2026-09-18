package com.hellotravel.domain.persistence.model.param;

import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;

/**
 * Conversation完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record ConversationWriteParam(ConversationAggregate aggregate) {
}
