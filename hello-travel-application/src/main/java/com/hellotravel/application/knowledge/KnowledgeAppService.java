package com.hellotravel.application.knowledge;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 承载KnowledgeApplication的受控业务契约。
 *
 * @author AIGenerator
 */
@Service
public final class KnowledgeAppService {

    /**
     * 已冻结的知识库动作到用例映射。
     *
     * @author AIGenerator
     */
    private final Map<String, KnowledgeActionHandler> actions;

    /**
     * 注册知识文档可用动作。
     *
     * @param candidates Spring发现的知识库动作应用
     * @author AIGenerator
     */
    public KnowledgeAppService(List<KnowledgeActionHandler> candidates) {
        Map<String, KnowledgeActionHandler> registered = new HashMap<>();
        for (KnowledgeActionHandler candidate : candidates) {
            if (registered.putIfAbsent(candidate.action(), candidate) != null) {
                throw new IllegalStateException("duplicate knowledge action: " + candidate.action());
            }
        }
        this.actions = Map.copyOf(registered);
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param knowledgeCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<KnowledgeAppResult> manage(KnowledgeCommand knowledgeCommand) {
        try {
            // 1. 列表查询必须有界；写动作不依赖分页字段，不能被无关默认值拒绝。
            if ("LIST".equals(knowledgeCommand.action())
                    && (knowledgeCommand.limit() < 1 || knowledgeCommand.limit() > 100)) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 2. 未注册动作立即拒绝，防止默认分支改变文档状态。
            KnowledgeActionHandler application = actions.get(knowledgeCommand.action());
            if (application == null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 3. 委托唯一用例执行。
            KnowledgeAppResult result = application.execute(knowledgeCommand);
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
