package com.hellotravel.application.knowledge.command;

/**
 * 用户私有资料操作。
 *
 * @param action 资料操作
 * @param userId 认证归属
 * @param documentId 公开资料标识
 * @param filename 展示文件名
 * @param bytes 有界附件
 * @param sourceUrl 来源链接
 * @param expectedVersion 预期版本
 * @param after 分页游标
 * @param limit 页大小
 * @author AIGenerator
 */
public record KnowledgeCommand(
        String action,
        Long userId,
        String documentId,
        String filename,
        byte[] bytes,
        String sourceUrl,
        Long expectedVersion,
        long after,
        int limit) {
        }
