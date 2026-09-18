package com.hellotravel.client.chat.response;

/**
 * 对话tab应用视图。
 *
 * @param id 公开ID
 * @param title 标题
 * @param version 实体版本
 * @param historyEpoch 删除代次
 * @param lastSeq 已分配最大消息序号
 * @param updatedAt UTC活动时间
 * @author AIGenerator
 */
public record ConversationResponse(
        String id, String title, long version, long historyEpoch, long lastSeq, String updatedAt) {
}
