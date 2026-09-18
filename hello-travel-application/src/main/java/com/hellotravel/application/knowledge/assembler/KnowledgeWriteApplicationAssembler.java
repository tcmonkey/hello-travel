package com.hellotravel.application.knowledge.assembler;

import com.hellotravel.domain.knowledge.model.aggregate.IndexJobAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeChunkAggregate;
import com.hellotravel.domain.knowledge.model.aggregate.KnowledgeDocumentAggregate;
import com.hellotravel.domain.knowledge.model.param.IndexJobWriteParam;
import com.hellotravel.domain.knowledge.model.param.KnowledgeChunkWriteParam;
import com.hellotravel.domain.knowledge.model.param.KnowledgeDocumentWriteParam;

import org.springframework.stereotype.Component;

/**
 * 知识库应用写入到本域参数的转换，不承担其他业务的映射。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeWriteApplicationAssembler {

    /**
     * 投影KnowledgeDocument写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeDocumentWriteParam write(KnowledgeDocumentAggregate aggregate) {
        return new KnowledgeDocumentWriteParam(aggregate);
    }

    /**
     * 投影KnowledgeChunk写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public KnowledgeChunkWriteParam write(KnowledgeChunkAggregate aggregate) {
        return new KnowledgeChunkWriteParam(aggregate);
    }

    /**
     * 投影IndexJob写入参数，聚合承担业务状态校验。
     *
     * @param aggregate 本次转换的aggregate快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public IndexJobWriteParam write(IndexJobAggregate aggregate) {
        return new IndexJobWriteParam(aggregate);
    }
}
