package com.hellotravel.application.chat;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatAppResult;
import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, ChatActionHandler> actions;

    /**
     * 注册动作到明确对话用例的固定映射。
     *
     * @param candidates Spring发现的对话动作应用
     * @author AIGenerator
     */
    public ChatAppService(List<ChatActionHandler> candidates) {
        Map<String, ChatActionHandler> registered = new HashMap<>();
        for (ChatActionHandler candidate : candidates) {
            if (registered.putIfAbsent(candidate.action(), candidate) != null) {
                throw new IllegalStateException("duplicate chat action: " + candidate.action());
            }
        }
        this.actions = Map.copyOf(registered);
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
            ChatActionHandler application = actions.get(chatCommand.action());
            if (application == null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 3. 委托已选择的用例执行具体业务。
            ChatAppResult result = application.execute(chatCommand);
            // 4. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
