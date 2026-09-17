package com.hellotravel.application.chat.result;

/**
 * 生成任务应用视图。
 *
 * @param id 任务公开ID
 * @param conversationId 对话公开ID
 * @param status 任务状态
 * @param error 安全错误分类
 * @param attempt 生成代次
 * @param context 上下文预算JSON
 * @author AIGenerator
 */
public record RunResult(
        String id,
        String conversationId,
        String status,
        String error,
        int attempt,
        String context) {
        }
