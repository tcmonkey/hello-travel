package com.hellotravel.adaptor.knowledge.output.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.application.knowledge.command.VectorItemCommand;
import com.hellotravel.application.knowledge.policy.KnowledgeIndexPolicy;
import com.hellotravel.model.knowledge.VectorDO;
import com.hellotravel.model.knowledge.VectorHitDO;

import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Milvus固定协议、请求字段及搜索响应双向转换。
 *
 * @author AIGenerator
 */
@Component
public final class VectorOutputConverter {

    /**
     * 项目专属集合名称。
     *
     * @author AIGenerator
     */
    public static final String COLLECTION = KnowledgeIndexPolicy.collection();

    /**
     * 固定向量维度。
     *
     * @author AIGenerator
     */
    public static final int DIMENSIONS = KnowledgeIndexPolicy.dimensions();

    /**
     * 单次嵌入与upsert批次上限。
     *
     * @author AIGenerator
     */
    public static final int MAX_BATCH = 16;

    /**
     * 有界检索候选数。
     *
     * @author AIGenerator
     */
    public static final int SEARCH_CANDIDATES = 12;

    /**
     * 验证公开ID，阻止注入Milvus过滤表达式。
     *
     * @param value 本次转换的value快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String id(String value) {
        // 1. 仅接受项目固定格式公开ID，禁止引号和表达式字符。
        if (value == null || !value.matches("[0-9A-HJKMNP-TV-Z]{26}")) {
            throw new AdaptorException(AdaptorErrorCode.INVALID);
        }
        // 2. 返回经过协议验证的ID供过滤表达式组装。
        return value;
    }

    /**
     * 绑定认证归属过滤表达式。
     *
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public String owner(VectorCommand command) {
        return "owner_user_id == \"" + id(command.ownerId()) + "\"";
    }

    /**
     * 构造固定检索策略请求，所有检索强制绑定当前用户。
     *
     * @param collection 本次转换的collection快照
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public SearchReq search(String collection, VectorCommand command) {
        // 1. 验证输入向量维度和有限值，非法向量不发送给Milvus。
        if (command.query() == null
                || command.query().size() != DIMENSIONS
                || command.query().stream()
                        .anyMatch(value -> value == null || !Float.isFinite(value))) {
            throw new AdaptorException(AdaptorErrorCode.INVALID);
        }
        // 2. 绑定用户过滤、固定字段和有界候选数。
        return SearchReq.builder()
                .collectionName(collection)
                .annsField("vector")
                .data(List.of(new FloatVec(command.query())))
                .filter(owner(command))
                .topK(SEARCH_CANDIDATES)
                .metricType(IndexParam.MetricType.COSINE)
                .searchParams(Map.of("ef", 64))
                .outputFields(List.of("document_id", "index_generation", "content_hash"))
                .build();
    }

    /**
     * 构造文档删除或旧代次清理请求，不扩大删除归属。
     *
     * @param collection 本次转换的collection快照
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public DeleteReq delete(String collection, VectorCommand command) {
        // 1. 删除条件始终绑定当前用户和文档。
        String filter = owner(command) + " and document_id == \"" + id(command.documentId()) + "\"";
        // 2. 清理模式只删除较旧代次，不删除当前有效索引。
        if ("RECONCILE".equals(command.action())) {
            if (command.generation() == null || command.generation() < 1) {
                throw new AdaptorException(AdaptorErrorCode.INVALID);
            }
            filter += " and index_generation < " + command.generation();
        }
        // 3. 返回SDK请求交由适配器执行网络操作。
        return DeleteReq.builder().collectionName(collection).filter(filter).build();
    }

    /**
     * 将有界批次投影为Milvus写入请求。
     *
     * @param collection 本次转换的collection快照
     * @param command 本次转换的command快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public UpsertReq upsert(String collection, VectorCommand command) {
        // 1. 检查批次容量和认证归属，再逐项映射固定schema字段。
        if (command.items() == null || command.items().size() > MAX_BATCH) {
            throw new AdaptorException(AdaptorErrorCode.INVALID);
        }
        String owner = id(command.ownerId());
        // 2. 每个写入项均校验文档绑定与租约代次，拒绝跨文档写入。
        var rows = command.items().stream().map(item -> row(command, owner, item)).toList();
        // 3. 返回SDK请求，网络调用和失败捕获保留在适配器。
        return UpsertReq.builder().collectionName(collection).data(rows).build();
    }

    /**
     * 转换单个向量项，映射前核验schema及批次归属。
     *
     * @param command 本次转换的command快照
     * @param owner 本次转换的owner快照
     * @param item 本次转换的item快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public JsonObject row(VectorCommand command, String owner, VectorItemCommand item) {
        // 1. 验证项ID、文档和代次一致性，拒绝不匹配的批次数据。
        id(item.key());
        id(item.documentId());
        if (!item.documentId().equals(command.documentId())
                || command.generation() == null
                || item.generation() != command.generation()
                || item.generation() < 1
                || item.vector() == null
                || item.vector().size() != DIMENSIONS
                || item.hash() == null
                || !item.hash().matches("[a-f0-9]{64}")) {
            throw new AdaptorException(AdaptorErrorCode.INVALID);
        }
        // 2. 映射固定schema字段，认证归属只来自批次命令。
        JsonObject row = new JsonObject();
        row.addProperty("vector_key", item.key());
        row.addProperty("owner_user_id", owner);
        row.addProperty("document_id", item.documentId());
        row.addProperty("index_generation", item.generation());
        row.addProperty("chunk_no", item.chunkNo());
        row.addProperty("content_hash", item.hash());
        // 3. 验证所有数值并转换为SDK向量数组。
        JsonArray vector = new JsonArray();
        for (Float value : item.vector()) {
            if (value == null || !Float.isFinite(value)) {
                throw new AdaptorException(AdaptorErrorCode.INVALID);
            }
            vector.add(value);
        }
        row.add("vector", vector);
        return row;
    }

    /**
     * 转换Milvus完整搜索响应，保留文档、代次与内容摘要供二次校验。
     *
     * @param response 本次转换的response快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorDO searchResult(SearchResp response) {
        // 1. 空搜索响应转换为空候选，禁止访问不存在的第一批结果。
        if (response.getSearchResults() == null || response.getSearchResults().isEmpty()) {
            return completed();
        }
        // 2. 投影本次唯一检索向量对应的候选元数据。
        var hits = response.getSearchResults().get(0).stream().map(this::hit).toList();
        return new VectorDO(hits);
    }

    /**
     * 投影单个SDK搜索候选。
     *
     * @param hit 本次转换的hit快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorHitDO hit(SearchResp.SearchResult hit) {
        return new VectorHitDO(
                String.valueOf(hit.getId()),
                String.valueOf(hit.getEntity().get("document_id")),
                ((Number) hit.getEntity().get("index_generation")).longValue(),
                String.valueOf(hit.getEntity().get("content_hash")),
                hit.getScore());
    }

    /**
     * 返回已完成写入或删除的能力结果。
     *
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public VectorDO completed() {
        return new VectorDO(List.of());
    }
}
