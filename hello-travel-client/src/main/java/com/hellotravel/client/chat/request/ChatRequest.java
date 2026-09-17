package com.hellotravel.client.chat.request;

/**
 * 对话操作参数，归属只能来自认证上下文。
 *
 * @param conversationId 会话标识
 * @param runId 任务标识
 * @param title 标题
 * @param text 用户输入
 * @param requestKey 请求UUID
 * @param messageIds 待删除消息
 * @param expectedVersion 预期版本
 * @param maxSeq 固定上界
 * @param historyEpoch 删除代次
 * @param after 分页游标
 * @param limit 页大小
 * @author AIGenerator
 */
public record ChatRequest(
        String conversationId,
        String runId,
        String title,
        String text,
        String requestKey,
        java.util.List<String> messageIds,
        Long expectedVersion,
        Long maxSeq,
        Long historyEpoch,
        Long after,
        Integer limit) {
        }
