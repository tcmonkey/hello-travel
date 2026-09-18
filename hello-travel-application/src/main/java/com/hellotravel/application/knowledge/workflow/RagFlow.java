package com.hellotravel.application.knowledge.workflow;

import com.hellotravel.application.exception.ApplicationErrorCode;
import com.hellotravel.application.exception.ApplicationException;
import com.hellotravel.application.knowledge.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.assembler.VectorCommandAssembler;
import com.hellotravel.application.model.adaptor.ModelOutAdaptor;
import com.hellotravel.application.model.assembler.ModelCommandAssembler;
import com.hellotravel.application.persistence.TravelRepositories;
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
    private final ModelCommandAssembler modelCommandAssembler;
    private final VectorCommandAssembler vectorCommandAssembler;

    public RagFlow(
            TravelRepositories repositories,
            ModelOutAdaptor model,
            VectorOutAdaptor vectors,
            ModelCommandAssembler modelCommandAssembler,
            VectorCommandAssembler vectorCommandAssembler) {
        this.repositories = repositories;
        this.model = model;
        this.vectors = vectors;
        this.modelCommandAssembler = modelCommandAssembler;
        this.vectorCommandAssembler = vectorCommandAssembler;
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
        // 1. 依据删除状态与当前记忆代次处理分支，避免继续使用无效数据。
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
        // 2. 取得本次请求的嵌入结果，供本段后续处理使用。
        var embedding = model.generate(modelCommandAssembler.embed(List.of(query)));
        // 3. 核对嵌入结果数量与输入批次一致，缺失结果不得继续写索引。
        if (!embedding.success() || embedding.data().vectors().isEmpty()) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
        // 4. 按可信内部标识读取账号当前快照。
        String owner = repositories.userAccount.findById(userId).entity().publicId();
        var result =
                vectors.index(
                        vectorCommandAssembler.search(owner, embedding.data().vectors().get(0)));
        // 5. 核对下层标准结果的成功状态，失败中止当前处理。
        if (!result.success()) {
            throw new ApplicationException(ApplicationErrorCode.UNAVAILABLE);
        }
        // 6. 取得本次知识检索的候选集合，供本段后续处理使用。
        List<Map<String, String>> matches = new java.util.ArrayList<>();
        // 7. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
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
        // 8. 返回本段实际处理结果，保持本层输出契约。
        return List.copyOf(matches);
    }
}
