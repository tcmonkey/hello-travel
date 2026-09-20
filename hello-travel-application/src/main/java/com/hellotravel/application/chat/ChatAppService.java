package com.hellotravel.application.chat;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatAppResult;
import com.hellotravel.application.chat.usecase.ChatActionOperations;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;

/**
 * 对话应用入口，协议层通过assembler调用。
 *
 * @author AIGenerator
 */
@Service
public final class ChatAppService {

    /**
     * 已冻结的对话动作到用例映射。
     *
     * @author AIGenerator
     */
    private final Map<String, Function<ChatCommand, ChatAppResult>> actions;

    /**
     * 注册动作到明确对话用例的固定映射。
     *
     * @param operations 对话用例操作集合
     * @author AIGenerator
     */
    public ChatAppService(ChatActionOperations operations) {
        this.actions = Map.ofEntries(
                Map.entry("BOOTSTRAP", operations::bootstrap),
                Map.entry("LIST", operations::list),
                Map.entry("CREATE", operations::create),
                Map.entry("RENAME", operations::rename),
                Map.entry("DELETE", command -> operations.erase(command, true)),
                Map.entry("DELETE_MESSAGES", command -> operations.erase(command, false)),
                Map.entry("HISTORY", operations::history),
                Map.entry("SUBMIT", operations::submit),
                Map.entry("RUN", operations::readRun),
                Map.entry("CANCEL", operations::cancel),
                Map.entry("RETRY", operations::retry),
                Map.entry("CONTEXT", operations::context));
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param chatCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<ChatAppResult> manage(ChatCommand chatCommand) {
        try {
            // 1. 统一校验分页上限，防止任一读取用例绕过有界查询约束。
            if (chatCommand.limit() < 1 || chatCommand.limit() > 200) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 查找动作唯一对应的用例，未知动作没有默认处理路径。
            Function<ChatCommand, ChatAppResult> application = actions.get(chatCommand.action());
            if (application == null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 3. 委托已选择的用例执行具体业务。
            ChatAppResult result = application.apply(chatCommand);
            // 4. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
