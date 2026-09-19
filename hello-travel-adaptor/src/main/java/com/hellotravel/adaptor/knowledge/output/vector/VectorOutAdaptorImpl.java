package com.hellotravel.adaptor.knowledge.output.vector;

import com.hellotravel.adaptor.exception.AdaptorErrorCode;
import com.hellotravel.adaptor.exception.AdaptorException;
import com.hellotravel.adaptor.knowledge.output.vector.converter.VectorOutputConverter;
import com.hellotravel.application.knowledge.vector.adaptor.VectorOutAdaptor;
import com.hellotravel.application.knowledge.vector.command.VectorCommand;
import com.hellotravel.common.error.Failures;
import com.hellotravel.common.result.Result;
import com.hellotravel.model.knowledge.VectorDO;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

    private final VectorOutputConverter vectorOutputConverter;

    public VectorOutAdaptorImpl(
            Environment environment, VectorOutputConverter vectorOutputConverter) {
        this.environment = environment;
        this.vectorOutputConverter = vectorOutputConverter;
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
            // 1. 选择冻结的项目专属集合，映射时先校验所有输入。
            String collection = VectorOutputConverter.COLLECTION;
            // 2. 按操作先转换请求，再准备客户端执行IO；非法请求不访问Milvus。
            return switch (vectorCommand.action()) {
                case "UPSERT" -> {
                    // 1. 映射完整批次并验证归属、维度和代次。
                    var request = vectorOutputConverter.upsert(collection, vectorCommand);
                    // 2. 仅在验证通过后初始化专属集合并提交批次。
                    var client = initializedClient(collection);
                    client.upsert(request);
                    yield Result.success(vectorOutputConverter.completed());
                }
                case "SEARCH" -> {
                    // 1. 将认证归属和有效向量投影为SDK请求。
                    var request = vectorOutputConverter.search(collection, vectorCommand);
                    // 2. 执行搜索并由converter投影候选元数据。
                    var response = initializedClient(collection).search(request);
                    yield Result.success(vectorOutputConverter.searchResult(response));
                }
                case "DELETE", "RECONCILE" -> {
                    // 1. 绑定文档归属与旧代次删除栅栏。
                    var request = vectorOutputConverter.delete(collection, vectorCommand);
                    // 2. 在专属集合执行受限删除，保持其他集合及当前代次。
                    initializedClient(collection).delete(request);
                    yield Result.success(vectorOutputConverter.completed());
                }
                default -> throw new AdaptorException(AdaptorErrorCode.INVALID);
            };
        } catch (Exception exception) {
            return Failures.capture(exception, AdaptorErrorCode.UNAVAILABLE);
        }
    }

    /**
     * 准备已通过输入验证的向量操作所需客户端和专属集合。
     *
     * @param collection 冻结的项目专属集合
     * @return 已完成schema核验的客户端
     * @author AIGenerator
     */
    private MilvusClientV2 initializedClient(String collection) {
        // 1. 获取带超时限制且可复用的客户端。
        var client = client();
        // 2. 核验专属集合schema，禁止结构漂移时继续执行IO。
        initialize(client, collection);
        return client;
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
                            .dimension(VectorOutputConverter.DIMENSIONS)
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
                || schema.getField("vector").getDimension() != VectorOutputConverter.DIMENSIONS
                || schema.getField("vector").getDataType() != DataType.FloatVector) {
            throw new IllegalStateException("collection drift");
        }
        // 4. 执行loadCollection职责步骤，并把失败交给所属事务或入口处理。
        client.loadCollection(LoadCollectionReq.builder().collectionName(collection).build());
    }
}
