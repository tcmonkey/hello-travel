package com.hellotravel.application.chat.sync.result;

/**
 * 不包含完整私密正文的持久通知。
 *
 * @param seq 事件序号
 * @param type 事件类型
 * @param id 目标公开ID
 * @param version 实体版本
 * @param targetSid 定向会话
 * @param payload 仅ID和状态载荷
 * @author AIGenerator
 */
public record SyncEventResult(
        long seq, String type, String id, long version, String targetSid, String payload) {
}
