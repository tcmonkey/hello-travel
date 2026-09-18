package com.hellotravel.adaptor.http.assembler;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.adaptor.http.support.HttpProtocol;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.chat.result.ConversationResult;
import com.hellotravel.application.chat.result.MessageResult;
import com.hellotravel.application.chat.result.RunResult;
import com.hellotravel.client.chat.request.ChatRequest;
import com.hellotravel.client.chat.response.ChatResponse;
import com.hellotravel.client.chat.response.ConversationResponse;
import com.hellotravel.client.chat.response.MessageResponse;
import com.hellotravel.client.chat.response.RunResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * 本层chat协议双向映射。
 *
 * @author AIGenerator
 */
@Component
public final class ChatInputAssembler {

    /**
     * 整体绑定请求与可信身份，集中解释协议路由及分页默认值。
     *
     * @param action 本次转换的action快照
     * @param request 本次转换的request快照
     * @param http 本次转换的http快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatCommand toCommand(String action, ChatRequest request, HttpServletRequest http) {
        // 1. 解释允许的协议路由，未知动作在本层拒绝。
        String selected =
                switch (action) {
                    case "bootstrap" -> "BOOTSTRAP";
                    case "list" -> "LIST";
                    case "create" -> "CREATE";
                    case "rename" -> "RENAME";
                    case "history" -> "HISTORY";
                    case "submit" -> "SUBMIT";
                    case "run" -> "RUN";
                    case "cancel" -> "CANCEL";
                    case "retry" -> "RETRY";
                    case "delete" -> "DELETE";
                    case "delete-messages" -> "DELETE_MESSAGES";
                    case "context" -> "CONTEXT";
                    default -> throw new AdaptorException(AdaptorErrorCode.NOT_FOUND);
                };
        // 2. 将协议字段投影为用例命令，内部归属只来自认证上下文。
        return new ChatCommand(
                selected,
                HttpIdentity.user(http),
                HttpIdentity.session(http),
                request.conversationId(),
                request.runId(),
                request.title(),
                request.text(),
                request.requestKey(),
                request.messageIds(),
                request.expectedVersion(),
                HttpProtocol.cursor(request.after()),
                request.maxSeq(),
                request.historyEpoch(),
                HttpProtocol.pageSize(request.limit()));
    }

    /**
     * 投影Conversation公开字段。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ConversationResponse conversation(ConversationResult value) {
        return new ConversationResponse(
                value.id(),
                value.title(),
                value.version(),
                value.historyEpoch(),
                value.lastSeq(),
                value.updatedAt());
    }

    /**
     * 投影Message公开字段。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public MessageResponse message(MessageResult value) {
        return new MessageResponse(
                value.id(),
                value.seq(),
                value.role(),
                value.status(),
                value.content(),
                value.citations(),
                value.version());
    }

    /**
     * 投影Run公开字段。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public RunResponse run(RunResult value) {
        return value == null
                ? null
                : new RunResponse(
                        value.id(),
                        value.conversationId(),
                        value.status(),
                        value.error(),
                        value.attempt(),
                        value.context());
    }

    /**
     * 转换完整对话应用视图，协议层不接触实体或持久化对象。
     *
     * @param result 本次转换的result快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public ChatResponse toResponse(ChatResult result) {
        return new ChatResponse(
                result.conversations().stream().map(this::conversation).toList(),
                result.messages().stream().map(this::message).toList(),
                run(result.run()),
                result.nextCursor(),
                result.hasMore(),
                result.maxSeq(),
                result.historyEpoch(),
                result.syncSeq(),
                result.context());
    }
}
