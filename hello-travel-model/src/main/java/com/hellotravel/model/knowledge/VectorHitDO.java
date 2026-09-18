package com.hellotravel.model.knowledge;

/**
 * 向量候选，不可直接作为可用正文。
 *
 * @param key Milvus主键
 * @param documentId 文档公开ID
 * @param generation 索引代次
 * @param hash 正文哈希
 * @param score COSINE相似度
 * @author AIGenerator
 */
public record VectorHitDO(
        String key, String documentId, long generation, String hash, float score) {
}
