package com.hellotravel.application.chat.assembler;

import com.hellotravel.application.chat.result.ConversationResult;
import com.hellotravel.application.chat.result.MessageResult;
import com.hellotravel.application.chat.result.RunResult;
import com.hellotravel.domain.chat.model.entity.ChatRunEntity;
import com.hellotravel.domain.chat.model.entity.ConversationEntity;
import com.hellotravel.domain.chat.model.entity.MessageEntity;

import org.springframework.stereotype.Component;

/**
 * 对话内部快照转换为应用视图，不把实体或PO返回协议边界。
 *
 * @author AIGenerator
 */
@Component
public final class ChatApplicationAssembler {

    /**
     * 处理conversation对应的受控业务操作。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @return 当前操作的业务结果
     */
    public ConversationResult conversation(ConversationEntity entity) {
        return new ConversationResult(
                entity.publicId(),
                entity.title(),
                entity.version(),
                entity.historyEpoch(),
                entity.lastMessageSeq(),
                entity.lastActivityAt().toString() + "Z");
    }

    /**
     * 返回可公开的安全提示。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @return 当前操作的业务结果
     */
    public MessageResult message(MessageEntity entity) {
        return new MessageResult(
                entity.publicId(),
                entity.messageSeq(),
                entity.role(),
                entity.status(),
                entity.content(),
                entity.citationsJson(),
                entity.version());
    }

    /**
     * 处理run对应的受控业务操作。
     *
     * @author AIGenerator
     * @param entity 受控entity参数
     * @param conversationId 受控conversationId参数
     * @return 当前操作的业务结果
     */
    public RunResult run(ChatRunEntity entity, String conversationId) {
        return new RunResult(
                entity.publicId(),
                conversationId,
                entity.status(),
                entity.errorCode(),
                entity.attemptCount(),
                entity.contextSnapshotJson());
    }
}
