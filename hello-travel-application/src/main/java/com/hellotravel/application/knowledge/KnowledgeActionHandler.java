package com.hellotravel.application.knowledge;

import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;

/**
 * 一个知识库动作对应一个应用服务的策略契约。
 *
 * @author AIGenerator
 */
public interface KnowledgeActionHandler {

  /**
   * 返回该应用服务唯一处理的动作标识。
   *
   * @return 受控动作标识
   * @author AIGenerator
   */
  String action();

  /**
   * 执行唯一的知识库业务动作。
   *
   * @param command 已由输入层转换的命令
   * @return 该动作的应用结果
   * @author AIGenerator
   */
  KnowledgeAppResult execute(KnowledgeCommand command);
}
