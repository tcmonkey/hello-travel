package com.hellotravel.adaptor.knowledge.output;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.application.knowledge.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.command.VectorCommand;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.VectorDO;
import com.hellotravel.model.knowledge.VectorHitDO;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 仅操作hello_travel_kb_v1_d1024；结构漂移报错，不删除或重建既有集合。
 *
 * @author AIGenerator
 */
@Component
public final class VectorOutAdaptorImpl implements VectorOutAdaptor {

    private final Environment environment;

    /**
     * 保存cached对应的有界运行状态。
     *
     * @author AIGenerator
     */
    private volatile MilvusClientV2 cached;

    public VectorOutAdaptorImpl(Environment environment) {
        this.environment = environment;
    }

    /**
     * 在项目专用集合执行隔离向量操作。
     *
     * @author AIGenerator
     * @param vectorCommand 当前用例命令，归属来自服务端
     * @return 当前操作的业务结果
     */
    public Result<VectorDO> index(VectorCommand vectorCommand) {
        try {
            // 1. 取得有超时约束的第三方客户端，供本段后续处理使用。
            MilvusClientV2 client = client();
            String collection = "hello_travel_kb_v1_d1024";
            // 2. 执行initialize职责步骤，并把失败交给所属事务或入口处理。
            initialize(client, collection);
            // 3. 执行validateId职责步骤，并把失败交给所属事务或入口处理。
            validateId(vectorCommand.ownerId());
            // 4. 取得凭据对应的已验证账号，供本段后续处理使用。
            String owner = "owner_user_id == \"" + vectorCommand.ownerId() + "\"";
            // 5. 按可信业务动作分发独立分支，未知动作返回受控失败。
            switch (vectorCommand.action()) {
                case "UPSERT" -> {
                    upsert(client, collection, vectorCommand);
                }
                case "SEARCH" -> {
                    if (vectorCommand.query() == null || vectorCommand.query().size() != 1024) {
                        throw new IllegalArgumentException("dimension");
                    }
                    var response =
                            client.search(
                                    SearchReq.builder()
                                            .collectionName(collection)
                                            .annsField("vector")
                                            .data(List.of(new FloatVec(vectorCommand.query())))
                                            .filter(owner)
                                            .topK(12)
                                            .metricType(IndexParam.MetricType.COSINE)
                                            .searchParams(Map.of("ef", 64))
                                            .outputFields(
                                                    List.of(
                                                            "document_id",
                                                            "index_generation",
                                                            "content_hash"))
                                            .build());
                    List<VectorHitDO> hits = new ArrayList<>();
                    for (var hit : response.getSearchResults().get(0)) {
                        hits.add(
                                new VectorHitDO(
                                        String.valueOf(hit.getId()),
                                        String.valueOf(hit.getEntity().get("document_id")),
                                        ((Number) hit.getEntity().get("index_generation"))
                                                .longValue(),
                                        String.valueOf(hit.getEntity().get("content_hash")),
                                        hit.getScore()));
                    }
                    return Result.success(new VectorDO(List.copyOf(hits)));
                }
                case "DELETE", "RECONCILE" -> {
                    validateId(vectorCommand.documentId());
                    String filter =
                            owner + " and document_id == \"" + vectorCommand.documentId() + "\"";
                    if ("RECONCILE".equals(vectorCommand.action())) {
                        if (vectorCommand.generation() == null || vectorCommand.generation() < 1) {
                            throw new IllegalArgumentException("generation");
                        }
                        filter += " and index_generation < " + vectorCommand.generation();
                    }
                    client.delete(
                            DeleteReq.builder().collectionName(collection).filter(filter).build());
                }
                default -> throw new IllegalArgumentException("operation");
            }
            // 6. 将本层成功数据封装为标准结果，保持对外模型隔离。
            return Result.success(new VectorDO(List.of()));
        } catch (Exception exception) {
            return Result.failure(AdaptorErrorCode.UNAVAILABLE);
        }
    }

    private synchronized MilvusClientV2 client() {
        // 1. 复用已建立的Milvus客户端，避免每次查询重新创建连接。
        if (cached != null) {
            return cached;
        }
        // 2. 取得待完成的请求构建器，供本段后续处理使用。
        var builder =
                ConnectConfig.builder()
                        .uri(environment.getProperty("MILVUS_URI", "http://127.0.0.1:19530"))
                        .connectTimeoutMs(3000);
        String token = environment.getProperty("MILVUS_TOKEN");
        // 3. 配置鉴权令牌时才设置Milvus访问凭据。
        if (token != null && !token.isBlank()) {
            builder.token(token);
        }
        // 4. 更新本次处理的局部数据或上下文，后续步骤读取同一快照。
        cached = new MilvusClientV2(builder.build()).withTimeout(15, TimeUnit.SECONDS);
        // 5. 返回本段实际处理结果，保持本层输出契约。
        return cached;
    }

    @jakarta.annotation.PreDestroy
    private void closeClient() {
        // 1. 释放已创建的Milvus客户端，未创建时不触发外部连接。
        if (cached != null) {
            cached.close();
        }
    }

    private void initialize(MilvusClientV2 client, String collection) {
        // 1. 首次使用时创建项目专属集合，不修改机器上已有其他集合。
        if (!client.hasCollection(HasCollectionReq.builder().collectionName(collection).build())) {
            var schema = client.createSchema();
            schema.addField(
                    AddFieldReq.builder()
                            .fieldName("vector_key")
                            .dataType(DataType.VarChar)
                            .maxLength(26)
                            .isPrimaryKey(true)
                            .autoID(false)
                            .build());
            for (String field : List.of("owner_user_id", "document_id")) {
                schema.addField(
                        AddFieldReq.builder()
                                .fieldName(field)
                                .dataType(DataType.VarChar)
                                .maxLength(26)
                                .build());
            }
            schema.addField(
                    AddFieldReq.builder()
                            .fieldName("content_hash")
                            .dataType(DataType.VarChar)
                            .maxLength(64)
                            .build());
            for (String field : List.of("index_generation", "chunk_no")) {
                schema.addField(
                        AddFieldReq.builder().fieldName(field).dataType(DataType.Int64).build());
            }
            schema.addField(
                    AddFieldReq.builder()
                            .fieldName("vector")
                            .dataType(DataType.FloatVector)
                            .dimension(1024)
                            .build());
            IndexParam index =
                    IndexParam.builder()
                            .fieldName("vector")
                            .indexType(IndexParam.IndexType.HNSW)
                            .metricType(IndexParam.MetricType.COSINE)
                            .extraParams(Map.of("M", 16, "efConstruction", 128))
                            .build();
            client.createCollection(
                    CreateCollectionReq.builder()
                            .collectionName(collection)
                            .collectionSchema(schema)
                            .enableDynamicField(false)
                            .indexParams(List.of(index))
                            .build());
        }
        // 2. 取得待返回的旅行事项说明，供本段后续处理使用。
        var description =
                client.describeCollection(
                        DescribeCollectionReq.builder().collectionName(collection).build());
        var schema = description.getCollectionSchema();
        // 3. 校验专属集合字段与预期一致，结构不兼容时拒绝继续写入。
        if (!Set.copyOf(description.getFieldNames())
                        .equals(
                                Set.of(
                                        "vector_key",
                                        "owner_user_id",
                                        "document_id",
                                        "content_hash",
                                        "index_generation",
                                        "chunk_no",
                                        "vector"))
                || Boolean.TRUE.equals(description.getAutoID())
                || Boolean.TRUE.equals(description.getEnableDynamicField())
                || schema.getField("vector").getDimension() != 1024
                || schema.getField("vector").getDataType() != DataType.FloatVector) {
            throw new IllegalStateException("collection drift");
        }
        // 4. 执行loadCollection职责步骤，并把失败交给所属事务或入口处理。
        client.loadCollection(LoadCollectionReq.builder().collectionName(collection).build());
    }

    private void validateId(String value) {
        // 1. 只接受项目生成的公开标识，避免向量过滤表达式被任意文本注入。
        if (value == null || !value.matches("[0-9A-HJKMNP-TV-Z]{26}")) {
            throw new IllegalArgumentException("identifier");
        }
    }

    private void upsert(MilvusClientV2 client, String collection, VectorCommand vectorCommand) {
        // 1. 每次最多写入16个已验证分块，控制向量请求规模。
        if (vectorCommand.items() == null || vectorCommand.items().size() > 16) {
            throw new IllegalArgumentException("batch");
        }
        // 2. 读取当前用例的数据窗口，后续显式处理空结果。
        List<JsonObject> rows = new ArrayList<>();
        // 3. 逐项处理当前数据窗口，并在循环中核对可用状态与停止条件。
        for (var item : vectorCommand.items()) {
            validateId(item.key());
            validateId(item.documentId());
            if (item.generation() < 1
                    || item.vector().size() != 1024
                    || !item.hash().matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("vector schema");
            }
            JsonObject row = new JsonObject();
            row.addProperty("vector_key", item.key());
            row.addProperty("owner_user_id", vectorCommand.ownerId());
            row.addProperty("document_id", item.documentId());
            row.addProperty("index_generation", item.generation());
            row.addProperty("chunk_no", item.chunkNo());
            row.addProperty("content_hash", item.hash());
            JsonArray vector = new JsonArray();
            for (Float value : item.vector()) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("vector");
                }
                vector.add(value);
            }
            row.add("vector", vector);
            rows.add(row);
        }
        // 4. 执行upsert职责步骤，并把失败交给所属事务或入口处理。
        client.upsert(UpsertReq.builder().collectionName(collection).data(rows).build());
    }
}
