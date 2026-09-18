package com.hellotravel.adaptor.http.input;

import com.hellotravel.adaptor.http.support.ApiViews;
import com.hellotravel.adaptor.http.support.HttpIdentity;
import com.hellotravel.adaptor.http.support.HttpResults;
import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.service.ChatApplication;
import com.hellotravel.client.chat.request.ChatRequest;
import com.hellotravel.client.chat.response.ChatResponse;
import com.hellotravel.common.result.Result;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话命令入口，分页全部由稳定上界和删除代次控制。
 *
 * @author AIGenerator
 */
@RestController
@RequestMapping("/api/v1/chat")
public final class ChatController {

    private final ChatApplication application;

    private final ApiViews views;

    public ChatController(ChatApplication application, ApiViews views) {
        this.application = application;
        this.views = views;
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param action 受控action参数
     * @param chatRequest 已验证的公开请求参数
     * @param httpServletRequest HTTP请求及认证上下文
     * @return 当前操作的业务结果
     */
    @PostMapping("/{action}")
    public Result<ChatResponse> manage(
            @PathVariable String action,
            @Valid @RequestBody ChatRequest chatRequest,
            HttpServletRequest httpServletRequest) {
        try {
            // 1. 取得候选任务快照，领取时再次核验，供本段后续处理使用。
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
                        default -> throw new DomainException(DomainErrorCode.NOT_FOUND);
                    };
            var result =
                    application.manage(
                            new ChatCommand(
                                    selected,
                                    HttpIdentity.user(httpServletRequest),
                                    HttpIdentity.session(httpServletRequest),
                                    chatRequest.conversationId(),
                                    chatRequest.runId(),
                                    chatRequest.title(),
                                    chatRequest.text(),
                                    chatRequest.requestKey(),
                                    chatRequest.messageIds(),
                                    chatRequest.expectedVersion(),
                                    chatRequest.after() == null ? 0 : chatRequest.after(),
                                    chatRequest.maxSeq(),
                                    chatRequest.historyEpoch(),
                                    chatRequest.limit() == null ? 100 : chatRequest.limit()));
            // 2. 返回本段实际处理结果，保持本层输出契约。
            return views.respond(result, ChatResponse.class);
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
