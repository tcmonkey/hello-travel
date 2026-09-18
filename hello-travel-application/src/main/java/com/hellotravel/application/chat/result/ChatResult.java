package com.hellotravel.application.chat.result;

/**
 * 有界分页和状态结果，完整恢复由客户端遍历所有页。
 *
 * @param conversations 对话视图
 * @param messages 消息视图
 * @param run 生成任务
 * @param nextCursor 下一页游标
 * @param hasMore 是否还有下一页
 * @param maxSeq 固定恢复上界
 * @param historyEpoch 删除代次
 * @param syncSeq 用户事件高水位
 * @param context 上下文JSON
 * @author AIGenerator
 */
public record ChatResult(
        java.util.List<ConversationResult> conversations,
        java.util.List<MessageResult> messages,
        RunResult run,
        long nextCursor,
        boolean hasMore,
        long maxSeq,
        long historyEpoch,
        long syncSeq,
        String context) {
}
