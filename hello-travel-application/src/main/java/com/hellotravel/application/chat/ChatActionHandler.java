package com.hellotravel.application.chat;

import com.hellotravel.application.chat.command.ChatCommand;
import com.hellotravel.application.chat.result.ChatAppResult;

/**
 * 一个对话动作对应一个应用服务的策略契约。
 *
 * @author AIGenerator
 */
public interface ChatActionHandler {

  /**
   * 返回该应用服务唯一处理的动作标识。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  String action();

  /**
   * 执行唯一的对话业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 该动作的应用结果
   * @author AIGenerator
   */
  ChatAppResult execute(ChatCommand command);
}
