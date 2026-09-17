package com.hellotravel.application.knowledge.command;

/**
 * 向量upsert项。
 *
 * @param key 分块公开键
 * @param documentId 文档公开ID
 * @param generation 索引代次
 * @param chunkNo 块序
 * @param hash 正文SHA256十六进制
 * @param vector 固定1024维
 * @author AIGenerator
 */
public record VectorItemCommand(
        String key,
        String documentId,
        long generation,
        int chunkNo,
        String hash,
        java.util.List<Float> vector) {
        }
