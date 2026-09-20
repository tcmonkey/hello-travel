package com.hellotravel.application.chat.assembler;

import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.model.param.ChatRunWriteParam;
import com.hellotravel.domain.chat.model.param.ConversationWriteParam;
import com.hellotravel.domain.chat.model.param.MessageWriteParam;
import com.hellotravel.domain.chat.model.param.ModelInvocationWriteParam;

import org.springframework.stereotype.Component;

/**
 * 对话应用写入到本域参数的转换，不承担其他业务的映射。
 *
 * @author AIGenerator
 */
@Component
public final class ChatWriteAppAssembler {

    /**
     * 投影Conversation写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ConversationWriteParam write(ConversationAggregate aggregate) {
        return new ConversationWriteParam(aggregate);
    }

    /**
     * 投影Message写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MessageWriteParam write(MessageAggregate aggregate) {
        return new MessageWriteParam(aggregate);
    }

    /**
     * 投影ChatRun写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatRunWriteParam write(ChatRunAggregate aggregate) {
        return new ChatRunWriteParam(aggregate);
    }

    /**
     * 投影ModelInvocation写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ModelInvocationWriteParam write(ModelInvocationAggregate aggregate) {
        return new ModelInvocationWriteParam(aggregate);
    }
}
