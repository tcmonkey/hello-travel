package com.hellotravel.application.knowledge.usecase;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeResult;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * 知识库动作策略表；动作仅选择明确用例，不形成伪工作流。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeActionDispatcher {

    /**
     * 已冻结的知识库动作到用例映射。
     *
     * @author AIGenerator
     */
    private final Map<String, Function<KnowledgeCommand, KnowledgeResult>> actions;

    /**
     * 注册知识文档可用动作。
     *
     * @param operations 知识文档操作集合
     * @author AIGenerator
     */
    public KnowledgeActionDispatcher(KnowledgeActionOperations operations) {
        this.actions = Map.of(
                "LIST", operations::list,
                "READ", operations::read,
                "UPLOAD", operations::upload,
                "RETRY", operations::retry,
                "DELETE", operations::delete);
    }

    /**
     * 按可信动作选择唯一知识库用例。
     *
     * @param knowledgeCommand 输入层完成协议转换后的命令
     * @return 对应知识库用例的结果
     * @author AIGenerator
     */
    public KnowledgeResult dispatch(KnowledgeCommand knowledgeCommand) {
        // 1. 列表查询必须有界；写动作不依赖分页字段，不能被无关默认值拒绝。
        if ("LIST".equals(knowledgeCommand.action())
                && (knowledgeCommand.limit() < 1 || knowledgeCommand.limit() > 100)) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 2. 未注册动作立即拒绝，防止默认分支改变文档状态。
        Function<KnowledgeCommand, KnowledgeResult> application = actions.get(knowledgeCommand.action());
        if (application == null) {
            throw new ApplicationException(ApplicationErrorCode.INVALID);
        }
        // 3. 委托唯一用例执行。
        return application.apply(knowledgeCommand);
    }
}
