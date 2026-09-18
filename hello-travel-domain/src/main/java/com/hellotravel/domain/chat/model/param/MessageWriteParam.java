package com.hellotravel.domain.chat.model.param;

import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;

/**
 * Message完整聚合写入参数。
 *
 * @param aggregate 完整聚合
 * @author AIGenerator
 */
public record MessageWriteParam(MessageAggregate aggregate) {
}
