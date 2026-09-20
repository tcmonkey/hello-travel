package com.hellotravel.application.chat.adaptor;

import com.hellotravel.application.chat.command.TravelDialogueCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.chat.ChatModelDO;

/**
 * 普通旅行对话防腐端口。
 *
 * @author AIGenerator
 */
public interface TravelDialogueAdaptor {

    /**
     * 生成普通旅行对话或事实问答。
     *
     * @param travelDialogueCommand 普通对话命令
     * @return 模型回答
     * @author AIGenerator
     */
    Result<ChatModelDO> answer(TravelDialogueCommand travelDialogueCommand);
}
