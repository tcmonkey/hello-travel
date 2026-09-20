package com.hellotravel.application.auth;

import com.hellotravel.application.auth.command.AuthCommand;
import com.hellotravel.application.auth.result.AuthAppResult;

/**
 * 一个认证动作对应一个应用服务的策略契约。
 *
 * @author AIGenerator
 */
public interface AuthActionHandler {

  /**
   * 返回该应用服务唯一处理的动作标识。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  String action();

  /**
   * 执行唯一的认证业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 该动作的应用结果
   * @author AIGenerator
   */
  AuthAppResult execute(AuthCommand command);
}
