package com.hellotravel.application.knowledge.service;

import com.hellotravel.application.knowledge.command.KnowledgeCommand;
import com.hellotravel.application.knowledge.result.KnowledgeResult;
import com.hellotravel.application.knowledge.workflow.KnowledgeFlow;
import com.hellotravel.common.result.Result;

import org.springframework.stereotype.Service;

/**
 * 承载KnowledgeApplication的受控业务契约。
 *
 * @author AIGenerator
 */
@Service
public final class KnowledgeApplication {

    private final KnowledgeFlow flow;

    public KnowledgeApplication(KnowledgeFlow flow) {
        this.flow = flow;
    }

    /**
     * 执行指定归属下的业务用例。
     *
     * @author AIGenerator
     * @param knowledgeCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<KnowledgeResult> manage(KnowledgeCommand knowledgeCommand) {
        try {
            return Result.success(flow.perform(knowledgeCommand));
        } catch (Exception exception) {
            return com.hellotravel.application.support.ApplicationFailures.capture(exception);
        }
    }
}
