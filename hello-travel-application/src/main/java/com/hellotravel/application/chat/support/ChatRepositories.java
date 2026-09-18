package com.hellotravel.application.chat.support;

import com.hellotravel.domain.chat.repository.ChatRunRepository;
import com.hellotravel.domain.chat.repository.ConversationRepository;
import com.hellotravel.domain.chat.repository.MessageRepository;
import com.hellotravel.domain.chat.repository.ModelInvocationRepository;

import org.springframework.stereotype.Component;

/**
 * 对话应用读取所需的本域仓储端口，不聚合其他业务仓储。
 *
 * @author AIGenerator
 */
@Component
public final class ChatRepositories {

    public final ConversationRepository conversation;
    public final MessageRepository message;
    public final ChatRunRepository chatRun;
    public final ModelInvocationRepository modelInvocation;

    public ChatRepositories(
            ConversationRepository conversation,
            MessageRepository message,
            ChatRunRepository chatRun,
            ModelInvocationRepository modelInvocation) {
        this.conversation = conversation;
        this.message = message;
        this.chatRun = chatRun;
        this.modelInvocation = modelInvocation;
    }
}
