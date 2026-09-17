package com.hellotravel.client.knowledge.request;

/**
 * 资料查询和状态变更请求。
 *
 * @param documentId 资料公开标识
 * @param expectedVersion 预期版本
 * @param after 分页游标
 * @param limit 页大小
 * @author AIGenerator
 */
public record KnowledgeRequest(
        String documentId, Long expectedVersion, Long after, Integer limit) {
        }
