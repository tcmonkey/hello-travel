package com.hellotravel.domain.knowledge.model.value;

import com.hellotravel.domain.exception.DomainErrorCode;
import com.hellotravel.domain.exception.DomainException;

/**
 * 文档索引元数据的不可变快照，不定义供应商或SDKschema默认值。
 *
 * @param model 已选择的嵌入模型
 * @param dimensions 已选择的向量维度
 * @param collection 已选择的专属集合
 * @author AIGenerator
 */
public record EmbeddingProfileValue(String model, int dimensions, String collection) {
    /**
     * 核验完整索引元数据快照。
     *
     * @author AIGenerator
     */
    public EmbeddingProfileValue {
        // 1. 元数据必须完整且维度为正，避免记录不可恢复的索引配置。
        if (model == null
                || model.isBlank()
                || dimensions < 1
                || collection == null
                || collection.isBlank()) {
            throw new DomainException(DomainErrorCode.INVALID);
        }
    }

    /**
     * 记录已选择的嵌入配置，领域不猜测供应商默认值。
     *
     * @param model 本次转换的model快照
     * @param dimensions 本次转换的dimensions快照
     * @param collection 本次转换的collection快照
     * @return 明确用途的转换结果
     * @author AIGenerator
     */
    public static EmbeddingProfileValue selected(String model, int dimensions, String collection) {
        return new EmbeddingProfileValue(model, dimensions, collection);
    }
}
