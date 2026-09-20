package com.hellotravel.adaptor.chat.input;

import com.hellotravel.adaptor.chat.input.assembler.ChatAssembler;
import com.hellotravel.adaptor.common.HttpResults;
import com.hellotravel.application.chat.ChatAppService;
import com.hellotravel.client.chat.request.ChatRequest;
import com.hellotravel.client.chat.response.ChatResponse;
import com.hellotravel.common.result.Result;

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

    private final ChatAppService application;
    private final ChatAssembler chatAssembler;

    public ChatController(ChatAppService application, ChatAssembler chatAssembler) {
        this.application = application;
        this.chatAssembler = chatAssembler;
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
            // 1. 将协议路由和完整请求转换为具有可信身份的用例命令。
            var command = chatAssembler.toCommand(action, chatRequest, httpServletRequest);
            // 2. 执行应用入口，保留失败分类且不向外暴露内部载荷。
            var result = application.manage(command);
            if (!result.success()) {
                return HttpResults.failure(result);
            }
            // 3. 通过本层assembler投影公开响应。
            return Result.success(chatAssembler.toResponse(result.data()));
        } catch (Exception exception) {
            return HttpResults.capture(exception);
        }
    }
}
