package com.hellotravel.application.chat.service;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.chat.workflow.ChatFlow;
import com.hellotravel.application.support.ApplicationFailures;
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
            // 1. 取得本段结果并准备本层转换，随后显式核对成功状态。
            ChatResult result = flow.perform(chatCommand);
            // 2. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
