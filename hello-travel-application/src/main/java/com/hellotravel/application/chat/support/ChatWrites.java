package com.hellotravel.application.chat.support;

import com.hellotravel.application.chat.assembler.ChatWriteApplicationAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.domain.chat.model.aggregate.ChatRunAggregate;
import com.hellotravel.domain.chat.model.aggregate.ConversationAggregate;
import com.hellotravel.domain.chat.model.aggregate.MessageAggregate;
import com.hellotravel.domain.chat.model.aggregate.ModelInvocationAggregate;
import com.hellotravel.domain.chat.service.ChatDomainService;

import org.springframework.stereotype.Component;

/**
 * 对话事务内部的领域写入协作；失败中断并交由所属应用入口捕获。
 *
 * @author AIGenerator
 */
@Component
public final class ChatWrites {

    private final ChatDomainService service;
    private final ChatWriteApplicationAssembler chatWriteApplicationAssembler;

    public ChatWrites(
            ChatDomainService service,
            ChatWriteApplicationAssembler chatWriteApplicationAssembler) {
        this.service = service;
        this.chatWriteApplicationAssembler = chatWriteApplicationAssembler;
    }

    /**
     * 校验Conversation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveConversation(ConversationAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = chatWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveConversation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验Message完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveMessage(MessageAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = chatWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveMessage(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验ChatRun完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveChatRun(ChatRunAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = chatWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveChatRun(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验ModelInvocation完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveModelInvocation(ModelInvocationAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = chatWriteApplicationAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveModelInvocation(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }
}
