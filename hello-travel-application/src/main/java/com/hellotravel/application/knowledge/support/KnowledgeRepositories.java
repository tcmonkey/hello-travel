package com.hellotravel.application.knowledge.support;

import com.hellotravel.domain.knowledge.repository.IndexJobRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeChunkRepository;
import com.hellotravel.domain.knowledge.repository.KnowledgeDocumentRepository;

import org.springframework.stereotype.Component;

/**
 * 知识库应用读取所需的本域仓储端口，不聚合其他业务仓储。
 *
 * @author AIGenerator
 */
@Component
public final class KnowledgeRepositories {

    public final KnowledgeDocumentRepository knowledgeDocument;
    public final KnowledgeChunkRepository knowledgeChunk;
    public final IndexJobRepository indexJob;

    public KnowledgeRepositories(
            KnowledgeDocumentRepository knowledgeDocument,
            KnowledgeChunkRepository knowledgeChunk,
            IndexJobRepository indexJob) {
        this.knowledgeDocument = knowledgeDocument;
        this.knowledgeChunk = knowledgeChunk;
        this.indexJob = indexJob;
    }
}
