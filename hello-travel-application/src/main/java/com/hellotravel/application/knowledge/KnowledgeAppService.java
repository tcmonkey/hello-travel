package com.hellotravel.application.knowledge;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeAppResult;
import com.hellotravel.application.knowledge.usecase.KnowledgeActionOperations;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;

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
    private final Map<String, Function<KnowledgeCommand, KnowledgeAppResult>> actions;

    /**
     * 注册知识文档可用动作。
     *
     * @param operations 知识文档操作集合
     * @author AIGenerator
     */
    public KnowledgeAppService(KnowledgeActionOperations operations) {
        this.actions = Map.of(
                "LIST", operations::list,
                "READ", operations::read,
                "UPLOAD", operations::upload,
                "RETRY", operations::retry,
                "DELETE", operations::delete);
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
            Function<KnowledgeCommand, KnowledgeAppResult> application = actions.get(knowledgeCommand.action());
            if (application == null) {
                throw new ApplicationException(ApplicationErrorCode.INVALID);
            }
            // 3. 委托唯一用例执行。
            KnowledgeAppResult result = application.apply(knowledgeCommand);
            return Result.success(result);
        } catch (Exception exception) {
            return ApplicationFailures.capture(exception);
        }
    }
}
