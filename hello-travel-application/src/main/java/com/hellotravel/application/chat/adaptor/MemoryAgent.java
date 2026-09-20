package com.hellotravel.application.chat.adaptor;

import com.hellotravel.application.chat.command.MemoryAgentCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;

/**
 * 会话记忆模型端口，只承担摘要和显式记忆提取。
 *
 * @author AIGenerator
 */
public interface MemoryAgent {

    /**
     * 压缩有界对话资料。
     *
     * @param memoryAgentCommand 已组装的摘要命令
     * @return 标准对话模型结果
     * @author AIGenerator
     */
    Result<ChatModelDO> summarize(MemoryAgentCommand memoryAgentCommand);

    /**
     * 提取用户明确要求保存的记忆事实。
     *
     * @param memoryAgentCommand 已组装的提取命令
     * @return 标准对话模型结果
     * @author AIGenerator
     */
    Result<ChatModelDO> extract(MemoryAgentCommand memoryAgentCommand);
}
