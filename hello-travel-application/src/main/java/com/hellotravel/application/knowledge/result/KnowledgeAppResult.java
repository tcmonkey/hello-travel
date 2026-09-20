package com.hellotravel.application.knowledge.result;

/**
 * 资料分页结果。
 *
 * @param items 分页结果
 * @param nextCursor 下一游标
 * @param hasMore 尚有后续
 * @author AIGenerator
 */
public record KnowledgeAppResult(
        java.util.List<DocumentAppResult> items, long nextCursor, boolean hasMore) {
}
