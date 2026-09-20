package com.hellotravel.application.knowledge.command;

/**
 * 专用Milvus collection操作。
 *
 * @param action 有界操作
 * @param ownerId 认证账号公开ID
 * @param documentId 文档公开ID
 * @param generation 当前有效代次
 * @param items 有界upsert批次
 * @param query 检索向量
 * @author AIGenerator
 */
public record VectorCommand(
        String action,
        String ownerId,
        String documentId,
        Long generation,
        java.util.List<VectorItemCommand> items,
        java.util.List<Float> query) {
}
