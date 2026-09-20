package com.hellotravel.application.knowledge;

import com.hellotravel.application.knowledge.assembler.KnowledgeWriteAppAssembler;
import com.hellotravel.application.support.ApplicationFailures;
import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.service.KnowledgeDomainService;

import org.springframework.stereotype.Component;

/**
 * 知识库事务内部的领域写入协作；失败中断并交由所属应用入口捕获。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeWriteAppService {

    private final KnowledgeDomainService service;
    private final KnowledgeWriteAppAssembler knowledgeWriteAppAssembler;

    public KnowledgeWriteAppService(
            KnowledgeDomainService service,
            KnowledgeWriteAppAssembler knowledgeWriteAppAssembler) {
        this.service = service;
        this.knowledgeWriteAppAssembler = knowledgeWriteAppAssembler;
    }

    /**
     * 校验KnowledgeDocument完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeDocument(KnowledgeDocumentAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = knowledgeWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveKnowledgeDocument(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验KnowledgeChunk完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveKnowledgeChunk(KnowledgeChunkAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = knowledgeWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveKnowledgeChunk(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }

    /**
     * 校验IndexJob完整聚合的版本、归属和删除状态后保存。
     *
     * @author AIGenerator
     * @param aggregate 待写入的完整聚合
     * @return 当前操作的业务结果
     */
    public Boolean saveIndexJob(IndexJobAggregate aggregate) {
        // 1. 用应用assembler绑定完整聚合或删除标识，禁止在调用处拼装Param。
        var param = knowledgeWriteAppAssembler.write(aggregate);
        // 2. 执行领域入口，状态、并发和不可变归属由领域负责。
        var result = service.saveIndexJob(param);
        // 3. 核验持久化结果，失败中断当前事务而不继续提交。
        return ApplicationFailures.required(result).saved();
    }
}
