package com.hellotravel.client.knowledge.response;

/**
 * 资料分页结果。
 *
 * @param items 分页结果
 * @param nextCursor 下一游标
 * @param hasMore 尚有后续
 * @author AIGenerator
 */
public record KnowledgeResponse(
        java.util.List<DocumentResponse> items, long nextCursor, boolean hasMore) {
        }
