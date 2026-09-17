package com.hellotravel.application.chat.service;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.chat.workflow.ChatFlow;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

/**
 * 对话应用入口，协议层通过assembler调用。
 *
 * @author AIGenerator
 */
@Service
public final class ChatApplication {

    private final ChatFlow flow;

    public ChatApplication(ChatFlow flow) {
        this.flow = flow;
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param chatCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<ChatResult> manage(ChatCommand chatCommand) {
        try {
            ChatResult result = flow.perform(chatCommand);
            return Result.success(result);
        } catch (Exception exception) {
            return com.hellotravel.application.support.ApplicationFailures.capture(exception);
        }
    }
}
