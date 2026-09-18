package com.hellotravel.application.chat.usecase;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * 对话动作策略表；只负责选择用例，不承载对话生命周期业务。
 *
 * @author AIGenerator
 */
@Component
public final class ChatActionDispatcher {

    /**
     * 已冻结的对话动作到用例映射。
     *
     * @author AIGenerator
     */
    private final Map<String, Function<ChatCommand, ChatResult>> actions;

    /**
     * 注册动作到明确对话用例的固定映射。
     *
     * @param operations 对话用例操作集合
     * @author AIGenerator
     */
    public ChatActionDispatcher(ChatActionOperations operations) {
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
     * 分派一个已转换的对话动作。
     *
     * @param chatCommand 输入层完成协议转换后的命令
     * @return 对应动作的结果
     * @author AIGenerator
     */
    public ChatResult dispatch(ChatCommand chatCommand) {
        // 1. 统一校验分页上限，防止任一读取用例绕过有界查询约束。
        if (chatCommand.limit() < 1 || chatCommand.limit() > 200) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 查找动作唯一对应的用例，未知动作没有默认处理路径。
        Function<ChatCommand, ChatResult> application = actions.get(chatCommand.action());
        if (application == null) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 3. 委托已选择的用例执行具体业务。
        return application.apply(chatCommand);
    }
}
