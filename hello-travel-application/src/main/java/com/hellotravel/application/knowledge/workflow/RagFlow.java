package com.hellotravel.application.knowledge.workflow;

import com.hellotravel.application.knowledge.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.application.model.adaptor.ModelOutAdaptor;
import com.hellotravel.application.model.command.ModelCommand;
import com.hellotravel.application.persistence.TravelRepositories;
import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;
import com.hellotravel.domain.query.model.value.QueryValue;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Milvus召回后必须经过MySQL归属、删除代次、就绪状态和内容摘要二次校验。
 *
 * @author AIGenerator
 */
@Component
public final class RagFlow {

    private final TravelRepositories repositories;

    private final ModelOutAdaptor model;

    private final VectorOutAdaptor vectors;

    public RagFlow(
            TravelRepositories repositories, ModelOutAdaptor model, VectorOutAdaptor vectors) {
        this.repositories = repositories;
        this.model = model;
        this.vectors = vectors;
    }

    /**
     * 处理retrieve对应的受控业务操作。
     *
     * @author AIGenerator
     * @param userId 认证账号主键
     * @param query 受控query参数
     * @return 当前操作的业务结果
     */
    public List<Map<String, String>> retrieve(Long userId, String query) {
        // 没有就绪的私有资料时不调用收费嵌入接口。
        if (repositories
                .knowledgeDocument
                .query(
                        QueryValue.all("id", 1)
                                .where("user_id", "EQ", userId)
                                .where("status", "EQ", "READY")
                                .where("deleted_at", "NULL", null))
                .isEmpty()) {
            return List.of();
        }
        var embedding =
                model.generate(new ModelCommand("EMBED", null, List.of(), List.of(query), null));
        if (!embedding.success() || embedding.data().vectors().isEmpty()) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
        String owner = repositories.userAccount.findById(userId).entity().publicId();
        var result =
                vectors.index(
                        new VectorCommand(
                                "SEARCH",
                                owner,
                                null,
                                0L,
                                List.of(),
                                embedding.data().vectors().get(0)));
        if (!result.success()) {
            throw new DomainException(DomainErrorCode.UNAVAILABLE);
        }
        List<Map<String, String>> matches = new java.util.ArrayList<>();
        for (var hit : result.data().hits()) {
            if (!Float.isFinite(hit.score()) || hit.score() < 0.55F) {
                continue;
            }
            var rows =
                    repositories.knowledgeChunk.query(
                            QueryValue.all("id", 1)
                                    .where("user_id", "EQ", userId)
                                    .where("vector_key", "EQ", hit.key())
                                    .where("deleted_at", "NULL", null)
                                    .where("status", "EQ", "READY"));
            if (rows.isEmpty()) {
                continue;
            }
            var chunk = rows.get(0).entity();
            var doc = repositories.knowledgeDocument.findById(chunk.documentId()).entity();
            if (!doc.userId().equals(userId)
                    || doc.deletedAt() != null
                    || !"READY".equals(doc.status())
                    || !doc.indexGeneration().equals(chunk.indexGeneration())
                    || !doc.indexGeneration().equals(hit.generation())
                    || !doc.publicId().equals(hit.documentId())
                    || !java.util.HexFormat.of()
                            .formatHex(chunk.contentSha256())
                            .equals(hit.hash())) {
                continue;
            }
            matches.add(
                    Map.of(
                            "id",
                            chunk.publicId(),
                            "documentId",
                            doc.publicId(),
                            "title",
                            doc.title(),
                            "text",
                            chunk.content(),
                            "url",
                            doc.sourceUrl() == null ? "" : doc.sourceUrl(),
                            "accessedAt",
                            doc.sourceAccessedAt() == null
                                    ? "用户上传，未独立核验"
                                    : doc.sourceAccessedAt().toString()));
            if (matches.size() == 4) {
                break;
            }
        }
        return List.copyOf(matches);
    }
}
