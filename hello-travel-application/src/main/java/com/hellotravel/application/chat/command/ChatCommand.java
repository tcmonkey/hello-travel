package com.hellotravel.application.chat.command;

/**
 * 对话与任务用例输入，归属由协议认证上下文派生。
 *
 * @param action 服务端固定业务动作
 * @param userId 认证账号
 * @param sessionId 原页面登录主键
 * @param conversationId 会话公开ID
 * @param runId 生成任务公开ID
 * @param title 标题
 * @param text 用户输入
 * @param requestKey 请求UUID
 * @param messageIds 有界删除ID集合
 * @param expectedVersion 状态CAS版本
 * @param after 稳定分页游标
 * @param maxSeq 消息快照序号上界
 * @param historyEpoch 删除代次
 * @param limit 有界页大小
 * @author AIGenerator
 */
public record ChatCommand(
        String action,
        Long userId,
        Long sessionId,
        String conversationId,
        String runId,
        String title,
        String text,
        String requestKey,
        java.util.List<String> messageIds,
        Long expectedVersion,
        long after,
        Long maxSeq,
        Long historyEpoch,
        int limit) {
}
